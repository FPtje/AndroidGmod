/*---------------------------------------------------------------------------
Android orientation framework

Author: (FPtje) Falco
---------------------------------------------------------------------------*/

local port = 54325 -- Static port. Better hope it's free!

local MsgTypes = {
	SYN = 0, -- Virtual connection request (part of two-way UDP handshake)
	ACK = 1, -- Virtual connection acknowledgement
	ORIENTATION = 2, -- Orienation (rotation) data
	ACCELERATION = 3, -- Acceleration (movement) data
	BUTTON = 4, -- Button presses and releases
	TEXT = 5 -- The text entered in the textbox on the top right
}

local messages = {}

/*---------------------------------------------------------------------------

HOOKS

---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------
onRead
When it receives a UDP datagram
---------------------------------------------------------------------------*/
local function onRead(socket, senderIP, senderport, buffer, err)
	if (err ~= GLSOCK_ERROR_SUCCESS) then return end -- Simply discard the packet if it hasn't arrived properly

	local count, byte = buffer:ReadByte() -- The type of message that is sent

	messages[byte](buffer, socket, senderIP, senderport) -- The function belonging to that message
	socket:ReadFrom(16, onRead) -- Read the next datagram
end

/*---------------------------------------------------------------------------
SYN/ACK
The android device would like to know if the host exists.
To figure out, a two-way handshake takes place.
---------------------------------------------------------------------------*/
messages[MsgTypes.SYN] = function(buffer, socket, senderIP, senderport)
	local send = GLSockBuffer()
	send:WriteByte(MsgTypes.ACK) -- ACK, the acknowledge
	socket:SendTo(send, senderIP, senderport, function() end)
end

/*---------------------------------------------------------------------------
Orientation
The android device sends its orientation data
this function deals with orientation data
---------------------------------------------------------------------------*/
messages[MsgTypes.ORIENTATION] = function(buffer)
	local _, p = buffer:ReadFloat(true)
	local _, y = buffer:ReadFloat(true)
	local _, r = buffer:ReadFloat(true)

	local angle = Angle(p, y, r)

	hook.Call("AndroidOrientation", nil, angle)
end

/*---------------------------------------------------------------------------
Acceleration
Much like orientation, but acceleration is the data of the accelerometer
---------------------------------------------------------------------------*/
messages[MsgTypes.ACCELERATION] = function(buffer)
	local _, x = buffer:ReadFloat(true)
	local _, y = buffer:ReadFloat(true)
	local _, z = buffer:ReadFloat(true)
	local direction = Vector(x, y, z * -1)

	hook.Call("AndroidAcceleration", nil, direction)
end

/*---------------------------------------------------------------------------
Button presses
The android device has 6 buttons. The user can press and release them
---------------------------------------------------------------------------*/
messages[MsgTypes.BUTTON] = function(buffer, socket)
	local _, buttonNr = buffer:ReadShort(true)
	local _, pressedByte = buffer:ReadByte()
	local pressed = pressedByte == 1 and true or false

	hook.Call("AndroidButton", nil, buttonNr, pressed)
end

/*---------------------------------------------------------------------------
Receiving text
There's a textbox on the top right of the screen. Whenever enter is pressed,
the text in that textbox is sent.
---------------------------------------------------------------------------*/
local collectedText = "" -- Text is not always sent in one datagram
messages[MsgTypes.TEXT] = function(buffer, socket)
	local size = buffer:Size()
	for i = 1, size - 1, 1 do
		local _, text = buffer:Read(1)
		collectedText = collectedText .. text

		if text == "\n" then
			hook.Call("AndroidText", nil, collectedText)
			collectedText = ""
		end
	end
end

/*---------------------------------------------------------------------------
onBound
Perform post-binding actions
---------------------------------------------------------------------------*/
local function onBound(socket, err)
	if err == GLSOCK_ERROR_SUCCESS then
		socket:ReadFrom(16, onRead)
	else
		Error("AndroidFramework: Could not create server socket! ("..tonumber(err)..")")
	end
end

/*---------------------------------------------------------------------------
CreateServer
Create the server that will listen for an android phone connection.
---------------------------------------------------------------------------*/
local ListenSocket
local function CreateServer()
	require("glsock") -- Requiring this file on top of the script causes it to malfunction
	ListenSocket = GLSock(GLSOCK_TYPE_UDP)
	ListenSocket:Bind("", port, onBound)
end

if SERVER then
	concommand.Add("Android_srvCreateServer", CreateServer)
	concommand.Add("Android_srvCloseServer", function() ListenSocket:Close() end)
else
	concommand.Add("Android_clCreateServer", CreateServer)
	concommand.Add("Android_clCloseServer", function() ListenSocket:Close() end)
end