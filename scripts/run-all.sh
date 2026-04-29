#!/bin/bash

docker-compose up -d

cd config-server

mvn clean package -DskipTests -X

cd ..

# Start Config Server first
echo "Starting Config Server..."
nohup java -jar config-server/target/config-server-0.0.1-SNAPSHOT.jar > logs/config.log 2>&1 &

# Wait for it to initialize - Uygulama 'UP' diyene kadar her 2 saniyede bir kontrol et
echo "Config Server başlatılıyor..."
until $(curl --output /dev/null --silent --head --fail http://localhost:8762/actuator/health); do
    printf '.'
    sleep 2
done
echo "Config Server başarıyla ayağa kalktı!"

cd discovery-server

mvn clean package -DskipTests -X

cd ..

# Start Service Registry (Eureka)
echo "Starting Eureka..."
nohup java -jar discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar > logs/discovery.log 2>&1 &

# Wait for it to initialize - Uygulama 'UP' diyene kadar her 2 saniyede bir kontrol et
echo "Discovery Server başlatılıyor..."
until $(curl --output /dev/null --silent --head --fail http://localhost:8761/actuator/health); do
    printf '.'
    sleep 2
done
echo "Discovery Server başarıyla ayağa kalktı!"


cd product-service

mvn clean package -DskipTests -X

cd ..

# Start a Business Service
echo "Starting Product Service..."
nohup java -jar product-service/target/product-service-0.0.1-SNAPSHOT.jar > logs/product.log 2>&1 &

# Wait for it to initialize - Uygulama 'UP' diyene kadar her 2 saniyede bir kontrol et
echo "Product Service başlatılıyor..."
until $(curl --output /dev/null --silent --head --fail http://localhost:8764/actuator/health); do
    printf '.'
    sleep 2
done
echo "Product Service başarıyla ayağa kalktı!"

echo "All services started."
