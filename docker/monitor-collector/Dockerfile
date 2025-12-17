FROM adoptopenjdk:8-jdk-hotspot

ARG profileActive
ENV profileActive=${profileActive}

ARG contextPath
ENV contextPath=${contextPath}

ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime

ARG os
ENV os=${os}
RUN if [[ '$os' == 'ubuntu' ]] ; then echo $TZ > /etc/timezone ; else echo $os ; fi

RUN mkdir -p ${contextPath}
WORKDIR ${contextPath}

EXPOSE 9999

ADD ./monitor-collector.jar ./

ENTRYPOINT ["sh","-c","java -Djava.security.egd=file:/dev/./urandom -jar ./monitor-collector.jar --spring.profiles.active=$profileActive"]
