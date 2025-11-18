FROM eclipse-temurin:21-jre

WORKDIR /app

# add libc and libgomp, these are required for XGBoost
RUN apt-get update
RUN apt-get install -y libc6 libgomp1

# copy the complete app folder
COPY app/server.jar /app/server.jar
COPY app/static/ /app/static/

# create uploads directory for work.dir
RUN mkdir -p /app/uploads

EXPOSE 3000

CMD ["java", "-XX:MaxRAMPercentage=80", "-XX:+UseG1GC", "-jar", "server.jar"]
