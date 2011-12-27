local anims = {
	ACT_GMOD_GESTURE_AGREE,
	ACT_GMOD_GESTURE_DISAGREE,
	ACT_GMOD_GESTURE_SALUTE,
	ACT_GMOD_GESTURE_BOW,
	ACT_GMOD_GESTURE_WAVE,
	ACT_GMOD_GESTURE_BECON
}

local animNames = {
	"Thumbs up",
	"Non-verbal no",
	"Salute",
	"Bow",
	"Wave",
	"Follow me!"
}

/*---------------------------------------------------------------------------
DarkRPButtons
Binds the Android buttons to performing animations
---------------------------------------------------------------------------*/
hook.Add("AndroidButton", "DarkRPButtons", function(button, pressed)
	if not pressed then -- when you release your dirty finger off your shiny device
		RunConsoleCommand("_DarkRP_DoAnimation", anims[button])
		chat.AddText(Color(230, 0, 0, 255), animNames[button])
	end
end)