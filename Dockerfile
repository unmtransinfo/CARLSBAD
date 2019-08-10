#
FROM ubuntu:18.10
WORKDIR /home/app
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update
RUN apt-get install -y apt-utils
ENV TZ=America/Denver
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ >/etc/timezone
RUN apt-get install -y tzdata
RUN apt-get install -y curl
RUN echo "=== Done installing Ubuntu."
#
RUN apt-get install -y openjdk-8-jdk
RUN apt-cache policy openjdk-8-jdk
RUN java -version
RUN echo "=== Done installing Java."
#
RUN apt-get install -y software-properties-common
RUN apt-add-repository -y "deb http://us.archive.ubuntu.com/ubuntu/ bionic universe"
RUN apt-get install -y tomcat9 tomcat9-admin
RUN apt-cache policy tomcat9
RUN cp -r /etc/tomcat9 /usr/share/tomcat9/conf
COPY conf/tomcat/tomcat-users.xml /usr/share/tomcat9/conf/
COPY conf/tomcat/server.xml /usr/share/tomcat9/conf/
RUN mkdir /usr/share/tomcat9/temp
RUN echo "=== Done installing Tomcat."
#
###
RUN apt-get install -y postgresql
RUN sudo -u postgres pg_config
RUN systemctl -l enable postgresql
COPY conf/postgresql/pg_hba.conf /etc/postgresql/10/main/
RUN chmod 640 /etc/postgresql/10/main/pg_hba.conf 
RUN systemctl -l start postgresql
RUN systemctl -l status postgresql
RUN echo "=== Done installing PostgreSQL."
#
RUN mkdir /home/data/carlsbad
COPY /home/data/carlsbad/carlsbad-pgdump.sql.gz /home/data/carlsbad
RUN sudo -u postgres createdb carlsbad
RUN gunzip -c /home/data/carlsbad/carlsbad-pgdump.sql.gz |sudo -u postgres psql -d carlsbad
RUN sudo -u postgres psql -d carlsbad -c "CREATE ROLE batman WITH LOGIN PASSWORD 'foobar'"
RUN sudo -u postgres psql -d carlsbad -c "GRANT SELECT ON ALL TABLES IN SCHEMA public TO ROLE batman"
RUN sudo -u postgres psql -d carlsbad -c "GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO ROLE batman"
RUN sudo -u postgres psql -d carlsbad -c "GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO ROLE batman"
RUN echo "=== Done loading database."
###
COPY carlsbad_war/target/carlsbad_war-0.0.1-SNAPSHOT.war /usr/share/tomcat9/webapps/carlsbad.war
RUN echo "=== Done installing application CARLSBAD."
#
CMD ["/usr/share/tomcat9/bin/catalina.sh", "run"]
#
