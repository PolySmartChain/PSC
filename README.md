`# Poly Smart Chain developer guide

1. git subtree usage

```shell script
git remote add someorigin https://github.com/someproject # add remote
git subtree add --prefix=foldername someorigin master # add folder MonadJ as subtree code directory
git subtree push --prefix=foldername someorigin master # push subtree local change to remote
git subtree pull --prefix=foldername someorigin master # pull from remote 
```

2. spring data jpa usage

https://docs.spring.io/spring-data/jpa/docs/current/reference/html/

3. lombok usage

https://projectlombok.org/

4. configurations override

-Dspring.config.location=classpath:\application.yml,some-path\custom-config.yml

5. web assembly text format compiler

https://github.com/WebAssembly/wabt/releases

6. smart contract development

- install rust toolchain https://www.rust-lang.org/tools/install
- install rust wasm build target ```rustup target add wasm32-unknown-unknown``` 
- build to wasm ```cargo build ```

7. RLP Encoding/Decoding see https://github.com/TrustedDataFramework/java-rlp

Benchmark compare to EthereumJ:

decoding list 10000000 times: 

ethereumJ: 3698ms 
our: 2515ms

8. github package

    1. set GITHUB_USERNAME environment as your github login name
    2. generate a token for github package: https://github.com/settings/tokens
    3. set GITHUB_TOKEN environment as the token generated above. 

9. vscode debug config example

```json
{
    "configurations": [
        {
            "type": "java",
            "name": "polysmartchain",
            "request": "launch",
            "mainClass": "org.tdf.sunflower.Start",
            "projectName": "polysmartchain-polysmartchain",
            "vmArgs": "-Dspring.config.location=polysmartchain/local/local-0.yml"
        }
    ]
}
```

## Commands

1. start application: (Windows) 

```.\gradlew polysmartchain:bootRun```

2. clear builds (Windows) 

```.\gradlew clean```

3. build and run fat jar (Windows)

```shell script
.\gradlew polysmartchain:bootJar       

# override default spring config with your custom config                     
java -jar polysmartchain\build\libs\polysmartchain*.jar -Dspring.config.location=classpath:\application.yml,some-path\custom-config.yml

# you can also load your config by environment
set SPRING_CONFIG_LOCATION=classpath:\application.yml,some-path\custom-config.yml 
java -jar consortium\build\libs\consortium-0.0.1-SNAPSHOT.jar
```  

4. build docker and push
 
```shell script
bash polysmartchain/docker/build.sh -i your_name_space/your_repository_name:tag --push
```

5. rest apis

- /rpc/account/{address} display account 
- /rpc/config display application configuration


## p2p 

1. message send by udp: ping, pong, status
2. peers are category as 
  - receive ping only: connection could only be established by other
  - receive pong only: connection could only be established by self
  - ping, pong both received, connection could be established by both


3. Ping/Pong = (0, string), (1, string)
4. the peer server maintain a websocket connection pool, only connect to peer received pong < 
5. sync message only send to connected peers, for messge < 512 byte, optimized to udp protocol
