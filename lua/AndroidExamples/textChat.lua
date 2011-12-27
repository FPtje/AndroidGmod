/*---------------------------------------------------------------------------
Text chat
Receive text from the Android phone and make the player say it.
---------------------------------------------------------------------------*/
hook.Add("AndroidText", "DoSay", function(text)
	RunConsoleCommand("say", text)
end)