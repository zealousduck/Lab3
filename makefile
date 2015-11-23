# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#									LAB 3 MAKEFILE							  #
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

all:
	make log
	make client
	make server
	cat todo.list
	
log:
	date
	ls -lt

client:
	javac Client.java

server:
	cc -o Server.out udp_server.c

clean:
	rm *.class
	rm *.out
