/*---------------------------------------------------------------------------
PreciseView
Set the view precisely by moving your finger over your android screen
---------------------------------------------------------------------------*/
local lastX, lastY = 0, 0
local pressed = true

/*---------------------------------------------------------------------------
AndroidFingerMovement

Called when the finger moves over the screen
---------------------------------------------------------------------------*/
hook.Add("AndroidFingerMovement", "PreciseView", function(x, y)
	local ang = EyeAngles()

	if not pressed then
		ang.y = ang.y - (x - lastX) / 20 -- Change the view slowly
		ang.p = ang.p + (y - lastY) / 20
	end

	LocalPlayer():SetEyeAngles(ang)

	lastX, lastY = x, y
	pressed = false
end)

/*---------------------------------------------------------------------------
AndroidButton
In this case only needed to figure out when the finger is not on the screen
If it doesn't, the view will reset every time you put your finger on the screen
---------------------------------------------------------------------------*/
hook.Add("AndroidButton", "ResetAngles", function(_, p)
	if p == false then
		pressed = true
	end
end)