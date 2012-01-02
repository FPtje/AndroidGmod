/*---------------------------------------------------------------------------
Voice Commands
Click the micophone button on your phone, say something and this hook will be called
---------------------------------------------------------------------------*/
hook.Add("AndroidText", "VoiceCommands", function(text)
	if string.find(text, "think") and string.find(text, "script") then
		RunConsoleCommand("gm_spawn", "models/props_junk/wood_crate001a.mdl")
	elseif string.find(text, "hate") and (string.find(text, "you") or string.find(text, "gary")) then
		RunConsoleCommand("kill")
		LocalPlayer():ChatPrint("KILL YOURSELF")
	end
end)