/*---------------------------------------------------------------------------
Move a prop using the acceleration
---------------------------------------------------------------------------*/

local entSelected = {}

/*---------------------------------------------------------------------------
selectProp
Select a prop to
---------------------------------------------------------------------------*/
local function selectProp(ply, cmd, args)
	local prop = ply:GetEyeTrace().Entity
	if IsValid(prop) then
		table.insert(entSelected, prop)
		MsgN("Added entity "..tostring(prop))
	end
end
concommand.Add("android_addentity", selectProp)

/*---------------------------------------------------------------------------
doAccelerate
Make the props move
---------------------------------------------------------------------------*/
local function doAccelerate(direction)
	for k,v in pairs(entSelected) do
		if not IsValid(v) then entSelected[k] = nil continue end
		local phys = v:GetPhysicsObject()

		if not phys:IsValid() then return end

		phys:AddVelocity(direction * 50)
	end
end
hook.Add("AndroidAcceleration", "moveProp", doAccelerate)