FROM cubestack/jdk1.8

COPY deploy /home/deploy/
EXPOSE 7000
EXPOSE 7070
EXPOSE 7077
EXPOSE 7010
EXPOSE 7017

ENTRYPOINT [ "/bin/bash", "-c", "cd /home/deploy && ./start.sh && tail -n 100 -f logs/*.out" ]

