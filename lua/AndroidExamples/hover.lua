local ScrWidth, ScrHeight = 1200, 800


local isDrawing = false
local isHovering = false

local lastEye = Angle(0) -- the eye angles when starting the hover
local lastX, lastY = 0, 0

/*---------------------------------------------------------------------------
Calcview hook that freezes the view while drawing
---------------------------------------------------------------------------*/
local function freezeView(ply, pos, angles, fov)
	local view = {}
	view.origin = pos
	view.angles = lastEye

	return view
end

/*---------------------------------------------------------------------------
AndroidButton
In this case only needed to figure out when the finger is not on the screen
---------------------------------------------------------------------------*/
hook.Add("AndroidButton", "DetectTouch", function(_, pressed)
	isDrawing = pressed

	if pressed then
		gui.EnableScreenClicker(true)
		gui.SetMousePos(lastX, lastY)

		hook.Add("CalcView", "FreezeView", freezeView)

		local aimVector = LocalPlayer():GetCursorAimVector():Angle()
		LocalPlayer():SetEyeAngles(aimVector)

		RunConsoleCommand("+attack")
		timer.Simple(0.05, function() RunConsoleCommand("-attack") end)
	else
		RunConsoleCommand("+attack")
		timer.Simple(0.05, function()
			RunConsoleCommand("-attack")
		end)
		timer.Simple(0, function() LocalPlayer():SetEyeAngles(lastEye) end)
	end
end)


/*---------------------------------------------------------------------------
AndroidHover
Detect hovering over the surface
---------------------------------------------------------------------------*/
hook.Add("AndroidHover", "DetectHover", function(down)
	isHovering = down

	if down then
		gui.EnableScreenClicker(true)
		gui.SetMousePos(lastX, lastY)

		lastEye = LocalPlayer():EyeAngles()

		local aimVector = LocalPlayer():GetCursorAimVector():Angle()
		LocalPlayer():SetEyeAngles(aimVector)
		hook.Add("CalcView", "FreezeView", freezeView)
	elseif not isDrawing then
		gui.EnableScreenClicker(false)
		LocalPlayer():SetEyeAngles(lastEye)
		hook.Remove("CalcView", "FreezeView")
	end
end)

/*---------------------------------------------------------------------------
AndroidFingerMovement
Moving the pen over the surface. Either hovering or not hovering
---------------------------------------------------------------------------*/
hook.Add("AndroidFingerMovement", "PenMovement", function(x, y)
	-- Switch x and y around
	x, y = y, x
	x, y = x / ScrWidth * ScrW(), (ScrHeight - y) / ScrHeight * ScrH()

	lastX, lastY = x, y

	local aimVector = LocalPlayer():GetCursorAimVector():Angle()
	if isDrawing or isHovering then
		gui.SetMousePos(x, y)
		LocalPlayer():SetEyeAngles(aimVector)
	end

	if isDrawing then
		RunConsoleCommand("+attack2")
		timer.Simple(0.05, function() RunConsoleCommand("-attack2") end)
	end
end)