FROM eclipse-temurin:25-jre

WORKDIR /app

# add libc and libgomp, these are required for XGBoost
RUN apt-get update
RUN apt-get install -y libc6 libgomp1

# copy the complete app folder
COPY app/server.jar /app/server.jar
COPY app/static/ /app/static/

# the data directory with the prediction model (model/) and uploads (uploads/);
# it is mounted from the host, see docker-compose.yaml
ENV APP_DATA_DIR=/app/data
RUN mkdir -p /app/data

EXPOSE 3000

CMD ["java", "-jar", "server.jar"]
