local calibrate
local lastAngle

/*---------------------------------------------------------------------------
getAngles
store the angles of the android device
---------------------------------------------------------------------------*/
local function getAngles(ang)
	lastAngle = Angle(ang.y, ang.p * -1, ang.r * -1)
end
hook.Add("AndroidOrientation", "view2", getAngles)

/*---------------------------------------------------------------------------
doCalibrate
calibrate the phone and store the angle at which the phone is looking at the screen
---------------------------------------------------------------------------*/
local function doCalibrate()
	calibrate = lastAngle
end
concommand.Add("Android_calibrate", doCalibrate)

/*---------------------------------------------------------------------------
stopView2
cancel this big toy
---------------------------------------------------------------------------*/
local function stopView2()
	hook.Remove("CalcView", "androidView")
	hook.Remove("AndroidOrientation", "view2")
	hook.Remove("RenderScreenspaceEffects", "view2")
end
concommand.Add("Android_stopview2", stopView2)

/*---------------------------------------------------------------------------
freeCalcView
sets the player's view to be different than where he's looking
---------------------------------------------------------------------------*/
local function freeCalcView(ply, origin, angles, fov)
	if not calibrate then return end

	local view = {
		origin = origin,
		angles = angles + (lastAngle - calibrate),
		fov = fov
	}

	return view
end
hook.Add("CalcView", "androidView", freeCalcView)

/*---------------------------------------------------------------------------
showLookingAt
With the script, it's hard to know where you're really looking
---------------------------------------------------------------------------*/
local lineMat = Material("cable/new_cable_lit")
local function showLookingAt()
	cam.Start3D(EyePos(), EyeAngles())
	render.SetMaterial(lineMat)
	render.DrawBeam(LocalPlayer():GetShootPos() - Vector(0,0,5), LocalPlayer():GetEyeTrace().HitPos, 0.2, 0.01, 1, Color(255, 255, 255, 255))
end
hook.Add("RenderScreenspaceEffects", "view2", showLookingAt)