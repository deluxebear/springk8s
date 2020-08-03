# spring boot k8s

## 此 demo 用来展示 spring cloud 在 k8s 上实现配置管理和服务发现

## 在 spring boot 项目引入相应的包：

```xml
				<dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-kubernetes-all</artifactId>
            <version>1.1.3.RELEASE</version>
        </dependency>

<!-- build 节点中增加 jib 来进行 docker image 的制造 -->
				<plugin>
                <groupId>com.google.cloud.tools</groupId>
                <artifactId>jib-maven-plugin</artifactId>
                <version>2.4.0</version>
                <configuration>
                    <from>
                        <image><!-- jdk 镜像 --></image>
                    </from>
                    <to>
                        <image><!-- 自己要封装的镜像 --></image>
                        <tags>

                            <tag>latest</tag>
                        </tags>
                    </to>
                    <container>
                        <creationTime>USE_CURRENT_TIMESTAMP</creationTime>
                    </container>
                </configuration>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>build</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
```

## 在 k8s 上需要部署 spring boot 的命名空间里面指定一个用户权限，这里的 YOUR-NAME-SPACE 改成自己的命名空间

```yaml
kind: Role
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  namespace: YOUR-NAME-SPACE
  name: namespace-reader
rules:
  - apiGroups: ["", "extensions", "apps"]
    resources: ["configmaps", "pods", "services", "endpoints", "secrets"]
    verbs: ["get", "list", "watch"]

---

kind: RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: namespace-reader-binding
  namespace: YOUR-NAME-SPACE
subjects:
- kind: ServiceAccount
  name: default
  apiGroup: ""
roleRef:
  kind: Role
  name: namespace-reader
  apiGroup: ""
```

## 在 resources 目录中新建 bootstrap.properties 里面内容:

```bash
#指定需要读取的k8s 里面 configMap 的名称
spring.cloud.kubernetes.config.name=springk8s
```

## 创建名为 springk8s 的 configMap

```yaml
kind: ConfigMap
apiVersion: v1
metadata:
  name: springk8s
  namespace: YOUR-NAME-SPACE
data:
  application.properties: |-
    server.port=8080
    #启用动态加载 configMap 里面的配置文件
    spring.cloud.kubernetes.reload.enabled=true
    #默认只会重载 @ConfigurationProperties 或 @RefreshScope 注解的 bean spring.cloud.kubernetes.reload.strategy=refresh 这是默认值
    #spring.cloud.kubernetes.reload.strategy=shutdown 这样会重启容器
    #spring.cloud.kubernetes.reload.strategy=restart_context 这样整个Spring ApplicationContext会正常重启，使用新配置重新创建Bean
		bean.message=Hello world!
```

## MyConfig.java 和 MyBean.java 用来测试默认 refresh 配置，项目部署后修改 configMap 里面的 bean.message 的内容会自动加载

```java
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bean")
public class MyConfig {
    private String message = "a message that can be changed live";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
```

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MyBean {
    @Autowired
    private MyConfig config;

    @Scheduled(fixedDelay = 5000)
    public void hello() {
        System.out.println("The message is: " + config.getMessage());
    }
}
```

## 定时任务需要在 Application 中启用： @EnableScheduling

```java
@SpringBootApplication
@EnableScheduling
public class Springk8sApplication {

    public static void main(String[] args) {
        SpringApplication.run(Springk8sApplication.class, args);
    }

}
```

## 修改 config.yaml 文件来启用 liveness 和 readiness

```yaml
kind: ConfigMap
apiVersion: v1
metadata:
  name: springk8s
  namespace: default
data:
  application.properties: |-
    server.port=8080
    #启用动态加载 configMap 里面的配置文件
    spring.cloud.kubernetes.reload.enabled=true
    #默认只会重载 @ConfigurationProperties 或 @RefreshScope 注解的 bean spring.cloud.kubernetes.reload.strategy=refresh 这是默认值
    #spring.cloud.kubernetes.reload.strategy=shutdown 这样会重启容器
    #spring.cloud.kubernetes.reload.strategy=restart_context 这样整个Spring ApplicationContext会正常重启，使用新配置重新创建Bean

    #启用 liveness 和 readiness
    management.health.defaults.enabled=true
    management.endpoint.health.probes.enabled=true
    management.health.livenessstate.enabled=true
    management.health.readinessstate.enabled=true
```

## 启用服务发现，在 Application 启用 @EnableDiscoveryClient

```java
@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
public class Springk8sApplication {

    public static void main(String[] args) {
        SpringApplication.run(Springk8sApplication.class, args);
    }

}
```

## 创建服务发现测试文件 DiscoveryService.java

```java
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscoveryService {
    private final DiscoveryClient discoveryClient;

    public DiscoveryService(DiscoveryClient discoveryClient){
        this.discoveryClient=discoveryClient;
    }

    @Scheduled(fixedDelay = 1000)
    public void getServiceInstance(){
        List<String> services=discoveryClient.getServices();
        for (String service:services
             ) {
            System.out.println("---------service name---------: "+service);
        }
    }
}
```

## 部署到 k8s 上，会将同命名空间下的所有 k8s service name 获得
