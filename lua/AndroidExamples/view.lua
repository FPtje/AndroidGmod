/*---------------------------------------------------------------------------
setView
set the view to the android device's angles
---------------------------------------------------------------------------*/
hook.Add("AndroidOrientation", "setView", function(ang)
	LocalPlayer():SetEyeAngles(Angle(ang.y, ang.p * -1, 0))
end)

/*---------------------------------------------------------------------------
controlPlayer
Until you grow a third hand to control the mouse while also holding the device,
the Android buttons will pretend to be mouse buttons
---------------------------------------------------------------------------*/
hook.Add("AndroidButton", "controlPlayer", function(b, p)
	if b == 1 then
		RunConsoleCommand((p and "+" or "-") .. "attack")
	elseif b == 2 then
		RunConsoleCommand((p and "+" or "-") .. "attack2")
	elseif b == 3 then
		RunConsoleCommand((p and "+" or "-") .. "reload")
	elseif b == 4 and not p then
		RunConsoleCommand("gmod_undo")
	elseif b == 5 then
		RunConsoleCommand((p and "+" or "-") .. "voicerecord")
	end
end)