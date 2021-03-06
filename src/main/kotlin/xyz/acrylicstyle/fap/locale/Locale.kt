package xyz.acrylicstyle.fap.locale

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.connection.ProxiedPlayer
import util.ReflectionHelper
import util.StringCollection
import util.promise.IPromise
import util.promise.Promise
import xyz.acrylicstyle.fap.FAP
import xyz.acrylicstyle.uls.api.Language
import xyz.acrylicstyle.uls.api.UniversalLanguageSelectorAPIProvider
import xyz.acrylicstyle.uls.bungee.UniversalLanguageSelectorImpl

@Suppress("LeakingThis")
abstract class Locale(id: String) {
    init {
        map[id] = this
    }

    companion object {
        @JvmStatic
        private val map = StringCollection<Locale>()
        @JvmStatic
        private var currentLocale: Locale? = null

        @Deprecated("use #getLocale(ProxiedPlayer) instead")
        @JvmStatic
        fun getLocale() = currentLocale ?: throw NullPointerException("Locale is not set.")

        @JvmStatic
        fun getLocale(player: ProxiedPlayer): Locale {
            try {
                //val obj = ReflectionHelper.invokeMethod(UniversalLanguageSelectorImpl::class.java, UniversalLanguageSelectorAPIProvider.getAPI(), "getLanguage", player.uniqueId)
                //@Suppress("UNCHECKED_CAST") val language = Promise.awaitT(IPromise::class.java.cast(obj) as IPromise<Any, *>, null) as Language
                val language = UniversalLanguageSelectorAPIProvider.getAPI().getLanguage(player.uniqueId).complete()
                if (language == Language.JAPANESE) return Locale_ja_JP
                if (language == Language.ENGLISH) return Locale_en_US
                return Locale_ja_JP
            } catch (e: NullPointerException) { return getLocale() }
        }

        @JvmStatic
        private fun setLocale(locale: Locale) { currentLocale = locale }

        @JvmStatic
        fun setLocale(localeId: String) = setLocale(map[localeId] ?: throw IllegalArgumentException("Could not find locale by $localeId"))
    }

    abstract val noPlayer: String
    abstract val invited: String
    abstract val notInParty: String
    abstract val invalidArgs: String
    abstract val inOtherParty: String
    abstract val joinedParty: String
    abstract val someoneJoinedParty: String
    abstract val noPermission: String
    abstract val partyDisbanded: String
    abstract val someoneLeftParty: String
    abstract val leftParty: String
    abstract val player: String
    abstract val partyHelpInvite: String
    abstract val partyHelpLeave: String
    abstract val partyHelpDisband: String
    abstract val partyHelpKick: String
    abstract val partyHelpList: String
    abstract val partyHelpPromote: String
    abstract val partyHelpWarp: String
    abstract val notInThisParty: String
    abstract val kickedFromParty: String
    abstract val kickedParty: String
    abstract val partyLeader: String
    abstract val partyMember: String
    abstract val offlinePlayer: String
    abstract val partyPromoted: String
    abstract val partyHijacked: String
    abstract val invitedParty: String
    abstract val invitedParty2: String
    abstract val clickToJoinParty: String
    abstract val partyInviteExpired: String
    abstract val partyInviteExpiredReceiver: String
    abstract val friendHelpAdd: String
    abstract val friendHelpAccept: String
    abstract val friendHelpRemove: String
    abstract val friendHelpList: String
    abstract val alreadyFriend: String
    abstract val clickToAcceptFR: String
    abstract val receivedFR: String
    abstract val receivedFR2: String
    abstract val sentFR: String
    abstract val noFR: String
    abstract val friendAccepted: String
    abstract val friendInviteExpired: String
    abstract val friendInviteExpiredReceiver: String
    abstract val notFriend: String
    abstract val removedFriend: String
    abstract val alreadyInParty: String
    abstract val partySummoned: String
    abstract val partyWarping: String
    abstract val cantWarp: String
    abstract val warpResult: String
    abstract val alreadySentFR: String
    abstract val privacyPartyTurnedOn: String
    abstract val privacyFriendTurnedOn: String
    abstract val privacyMessageTurnedOn: String
    abstract val privacyPartyTurnedOff: String
    abstract val privacyFriendTurnedOff: String
    abstract val privacyMessageTurnedOff: String
    abstract val cantSendPartyInvite: String
    abstract val cantSendFriendRequest: String
    abstract val cantSendMessage: String
    val partyAlreadyInServer = "${ChatColor.YELLOW}${FAP.CIRCLED_STAR}${ChatColor.GRAY}(%s): %s"
    val partyWarped = "${ChatColor.GREEN}${FAP.HEAVY_CHECK_MARK}${ChatColor.GRAY}(%s): %s"
    val partyWarpFailed = "${ChatColor.RED}${FAP.HEAVY_X}${ChatColor.GRAY}(%s): %s"
}
