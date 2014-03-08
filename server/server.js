
var express = require('express');
var app = express();
    http = require('http');

var server = http.createServer(app).listen(8081, "0.0.0.0");
interval = undefined;

//Game stuff
waiting = [];
playing = []
sockets = []
//Socket
var io = require('socket.io').listen(server);

io.sockets.on('connection', function(socket) {
	sockets[socket.id]=socket;
	if(waiting.length>0){
		console.log("User matched");
		info = {"partner":waiting[0], "turn":socket.id, "self":socket.id}
		playing[socket.id]=info;
		socket.emit("message_to_client", {"action":"found", "info":playing[socket.id]});
		waiting = waiting.splice(1, 1);
		console.log(waiting);
		console.log("Length: "+waiting.length);

	}else{
		waiting.push(socket.id);
		console.log(waiting);
	}
	console.log("Connection");
	
	socket.on('shareTablero', function(data) {
		console.log(socket.id);
		sockets[playing[socket.id]["partner"]].emit("message_to_client", {"action":"sendTablero", "partner":socket.id, "tablero":data['tablero']});
	
	});
	
	socket.on('sendMovement', function(data) {

		sockets[data['partner']].emit("message_to_client", {"action":"doMovement", "x":data['x'], "y":data['y']});
	
	});
	socket.on('sendMarked', function(data) {

		sockets[data['partner']].emit("message_to_client", {"action":"doMark", "x":data['x'], "y":data['y']});
	
	});	
	
	socket.on('disconnect', function() {
		for(p in playing){
			if(playing[p]["partner"]==socket.id){
				sockets[p].emit("message_to_client", {"action":"partnerEnd"});
				var index = playing.indexOf(p);
				if (index > -1) {
				    playing = playing.splice(index, 1);
				}
				var index2 = waiting.indexOf(socket.id);
				if (index > -1) {
				    waiting = waiting.splice(index2, 1);
				}

			}
		}
		try{
				sockets[playing[socket.id]['partner']].emit("message_to_client", {"action":"partnerEnd"});	
				var index = playing.indexOf(socket.id);
				if (index > -1) {
				    playing = playing.splice(index, 1);
				}
				var index2 = waiting.indexOf(socket.id);
				if (index > -1) {
				    waiting = waiting.splice(index2, 1);
				}		
		}
		catch(e){}
	
	});	

});

