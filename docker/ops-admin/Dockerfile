FROM docker.1ms.run/azul/zulu-openjdk:21

ARG profileActive
ENV profileActive=${profileActive}

ARG contextPath
ENV contextPath=${contextPath}

ARG uid
ENV uid=${uid}

ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime

RUN adduser park --uid ${uid}
USER park

RUN mkdir -p ${contextPath}
WORKDIR ${contextPath}

EXPOSE 8989

ADD ./bs-monitor.jar ./

ENTRYPOINT ["sh","-c","java -Djava.security.egd=file:/dev/./urandom -jar ./ops-admin.jar --spring.profiles.active=$profileActive"]

