local ScrWidth, ScrHeight = 1200, 800

local hasEverHovered = false
local isDrawing = false
local isHovering = false

local lastEye = Angle(0, 0, 0) -- the eye angles when starting the hover
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
hook.Add("AndroidButton", "DetectFingerTouch", function(_, pressed)
	isDrawing = pressed

	if pressed then
		gui.EnableScreenClicker(true)
		vgui.GetWorldPanel():SetWorldClicker(true)
		gui.SetMousePos(lastX, lastY)

		if not hasEverHovered then
			lastEye = LocalPlayer():EyeAngles()
		end

		hook.Add("CalcView", "FreezeView", freezeView)

		local aimVector = LocalPlayer():GetAimVector():Angle()
		LocalPlayer():SetEyeAngles(aimVector)

		RunConsoleCommand("+attack")
	else
		RunConsoleCommand("-attack")
		timer.Simple(0, function() LocalPlayer():SetEyeAngles(lastEye) end)
		if not hasEverHovered then
			timer.Simple(1, function()
				gui.EnableScreenClicker(false)
				vgui.GetWorldPanel():SetWorldClicker(false)
				LocalPlayer():SetEyeAngles(lastEye)
				hook.Remove("CalcView", "FreezeView")
			end)
		end
	end
end)


/*---------------------------------------------------------------------------
AndroidHover
Detect hovering over the surface. In this case optional
---------------------------------------------------------------------------*/
hook.Add("AndroidHover", "DetectOptionalHover", function(down)
	isHovering = down
	hasEverHovered = true -- Detect whether this is a drawing tablet

	if down then
		gui.EnableScreenClicker(true)
		gui.SetMousePos(lastX, lastY)

		vgui.GetWorldPanel():SetWorldClicker(true)
		lastEye = LocalPlayer():EyeAngles()

		local aimVector = LocalPlayer():GetAimVector():Angle()
		LocalPlayer():SetEyeAngles(aimVector)
		hook.Add("CalcView", "FreezeView", freezeView)
	elseif not isDrawing then
		gui.EnableScreenClicker(false)
		vgui.GetWorldPanel():SetWorldClicker(true)
		LocalPlayer():SetEyeAngles(lastEye)
		hook.Remove("CalcView", "FreezeView")
	end
end)

/*---------------------------------------------------------------------------
AndroidFingerMovement
Moving the pen over the surface. Either hovering or not hovering
---------------------------------------------------------------------------*/
hook.Add("AndroidFingerMovement", "FollowFingerMovement", function(x, y)
	-- Switch x and y around
	x, y = y, x
	x, y = x / ScrWidth * ScrW(), (ScrHeight - y) / ScrHeight * ScrH()

	lastX, lastY = x, y

	local aimVector = LocalPlayer():GetAimVector():Angle()
	if isDrawing or isHovering then
		gui.SetMousePos(x, y)
		LocalPlayer():SetEyeAngles(aimVector)
	end
end)