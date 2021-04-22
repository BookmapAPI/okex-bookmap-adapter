#gitlab.dev.bookmap.com:5050/bookmap/ ###

FROM gitlab.dev.bookmap.com:5050/bookmap/doker-containers/historical-data-server:0.3.25

ENV DEVELOPER_KEY=Bookmap-3azCFJXmO5nK5uM1DNcv
ENV SOURCE=EXTERNAL
ENV USERNAME=""
ENV PASSWORD=""
ENV IS_DEMO=true
ENV JAR_PATH=/home/hist-okex/okex-adapter.jar
ENV JAR_CLASS=com.stableapps.okex.OkexRelayProvider

COPY lib/okex-adapter-0.1.4.13.jar /home/hist-okex/okex-adapter.jar

COPY autorizedKeys/authorized_keys /root/.ssh/authorized_keys