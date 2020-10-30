package xyz.acrylicstyle.fap.locale

@Suppress("ClassName")
class Locale_en_US: Locale("en_US") {
    override val noPlayer = "Could not find player."
    override val invited = "%s invited %s to the party! They have 60 seconds to accept."
    override val notInParty = "You're not in party!"
    override val invalidArgs = "Invalid arguments."
    override val inOtherParty = "You can't invite this player as they're in other party."
    override val joinedParty = "You joined the %s party!"
    override val someoneJoinedParty = "%s joined the party!"
    override val noPermission = "You don't have permission to do this."
    override val partyDisbanded = "%s disbanded the party."
    override val someoneLeftParty = "%s left the party."
    override val leftParty = "You left the party."
    override val player = "Player"
    override val partyHelpInvite = "Invites a player to the party."
    override val partyHelpLeave = "Leaves a party."
    override val partyHelpDisband = "Disbands a party."
    override val partyHelpKick = "Kicks player from the party."
    override val partyHelpList = "Shows party members list."
    override val partyHelpPromote = "Promotes a player to party leader."
    override val partyHelpWarp = "Transfers party members to your server."
    override val notInThisParty = "This player is not in this party."
    override val kickedFromParty = "You were kicked from the party: %s"
    override val kickedParty = "Kicked %s from the party: %s"
    override val partyLeader = "Party Leader"
    override val partyMember = "Party Member"
    override val offlinePlayer = "This player is currently offline."
    override val partyPromoted = "%s promoted %s to the party leader!"
    override val partyHijacked = "%s hijacked the party!"
    override val invitedParty = "%s invited you to join the party!"
    override val invitedParty2 = "Click here to join the party!"
    override val clickToJoinParty = "Join the party"
    override val partyInviteExpired = "Party invite to %s has expired."
    override val partyInviteExpiredReceiver = "Party invite from %s has expired."
    override val friendHelpAdd = "Adds friend. They have to accept too."
    override val friendHelpAccept = "Accepts friend request."
    override val friendHelpRemove = "Removes friend from your friend list."
    override val friendHelpList = "Shows friend list."
    override val alreadyFriend = "This player is already friend!"
    override val clickToAcceptFR = "Click to add this player as friend!"
    override val receivedFR = "You have received friend request from %s!"
    override val receivedFR2 = "Click this message to add this player as friend!"
    override val sentFR = "You have sent friend request to %s! They have 3 minutes to accept."
    override val noFR = "There is no friend request from this player."
    override val friendAccepted = "You are now friend with %s."
    override val friendInviteExpired = "Friend request to %s has expired."
    override val friendInviteExpiredReceiver = "Friend request from %s has expired."
    override val notFriend = "This player isn't your friend!"
    override val removedFriend = "You have removed %s from your friend list!"
    override val alreadyInParty = "This player is already in the party, or pending invite."
    override val partySummoned = "Summoned to this server by %s"
    override val partyWarping = "Warping players..."
    override val cantWarp = "You can't summon players to this server!"
    override val warpResult = "Warp result:"
}