#FROM anapsix/alpine-java:8_server-jre_unlimited

FROM openjdk:17-ea-oracle

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

EXPOSE 8089

#ADD ./target/parking-inside-cloud-client.jar ./
ADD ./bs-monitor.jar ./
ADD ./bs-collector-release.jar ./

CMD java -jar bs-monitor.jar --spring.profiles.active=${profileActive:test} & java -jar bs-collector-release.jar --spring.profiles.active=${profileActive:test}
