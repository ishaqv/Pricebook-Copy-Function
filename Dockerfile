FROM mcr.microsoft.com/azure-functions/java:4-java11-build AS installer-env
COPY . /src/java-function-app
RUN cd /src/java-function-app && \
    mkdir -p /home/site/wwwroot && \
    chmod +x gradlew && \
    ./gradlew clean build && \
    ./gradlew azureFunctionsPackage && \
    cd ./build/azure-functions/ && \
    cd $(ls -d */|head -n 1) && \
    cp -a . /home/site/wwwroot

FROM mcr.microsoft.com/azure-functions/java:4-java11-appservice
# If you want to deploy outside of Azure, use this image
#FROM mcr.microsoft.com/azure-functions/java:3.0-java8
ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true
COPY --from=installer-env ["/home/site/wwwroot", "/home/site/wwwroot"]