@echo off
for %%d in (config-server discovery-server api-gateway user-service product-service shopping-cart-service order-service stock-service payment-service) do (
    pushd %%d
    call mvn clean compile jib:dockerBuild -DskipTests
    popd
)
