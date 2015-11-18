# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#									LAB 2 MAKEFILE							  #
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

all:
	make log
	make client
	make server
	
log:
	date
	ls -lt

client:
	cc -o UDPclient.out UDPclient.c

server:
	javac UDPserver.java

