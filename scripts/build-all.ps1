$services = @(
    "config-server",
    "discovery-server",
    "api-gateway",
    "user-service",
    "product-service",
    "shopping-cart-service",
    "order-service",
    "stock-service"
)

foreach ($service in $services) {
    Push-Location $service
    mvn clean compile jib:dockerBuild -DskipTests
    Pop-Location
}
