package dev.qtremors.eonslate.core.tracker.domain.finance

import java.util.Locale

/** A common subscription: its name, its brand color as ARGB, and its category. No logos ship. */
data class Service(val id: String, val name: String, val color: Long, val category: String)

object Services {
    const val PREFIX = "service:"

    val all: List<Service> = """
Netflix|E50914|Streaming
Amazon Prime|00A8E1|Streaming
Disney+ Hotstar|113CCF|Streaming
JioCinema|0A2540|Streaming
YouTube Premium|FF0000|Streaming
Sony LIV|000000|Streaming
Zee5|8B5CF6|Streaming
HBO Max|5822B4|Streaming
Hulu|1CE783|Streaming
Apple TV+|000000|Streaming
Crunchyroll|F47521|Streaming
Paramount+|0064FF|Streaming
Peacock|000000|Streaming
Nebula|2A40FF|Streaming
Spotify|1DB954|Music
Apple Music|FA243C|Music
YouTube Music|FF0000|Music
Amazon Music|25D1DA|Music
Tidal|000000|Music
Deezer|FEAA2D|Music
SoundCloud|FF5500|Music
Gaana|E72C30|Music
Wynk Music|EE4D5F|Music
iCloud|3693F3|Storage
Google One|4285F4|Storage
Dropbox|0061FF|Storage
OneDrive|0078D4|Storage
Box|0061D5|Storage
pCloud|01AFEF|Storage
Xbox Game Pass|107C10|Gaming
PlayStation Plus|003087|Gaming
Nintendo Online|E60012|Gaming
Steam|1B2838|Gaming
Epic Games|313131|Gaming
EA Play|000000|Gaming
Ubisoft+|000000|Gaming
GeForce NOW|76B900|Gaming
Apple Arcade|000000|Gaming
Google Play Pass|01875F|Gaming
Microsoft 365|05A6F0|Productivity
Notion|000000|Productivity
Canva|00C4CC|Productivity
Adobe Creative Cloud|FF0000|Productivity
Figma|F24E1E|Productivity
Miro|FFD02F|Productivity
Todoist|E44332|Productivity
Evernote|00A82D|Productivity
Grammarly|15C39A|Productivity
Linear|5E6AD2|Productivity
Airtable|18BFFF|Productivity
NordVPN|4687FF|VPN
ExpressVPN|DA3940|VPN
Surfshark|178BF4|VPN
ProtonVPN|6D4AFF|VPN
1Password|3B66BC|Security
LastPass|D32D27|Security
Bitwarden|175DDC|Security
Dashlane|0E353D|Security
Slack|4A154B|Communication
Discord Nitro|5865F2|Communication
Zoom|0B5CFF|Communication
Microsoft Teams|6264A7|Communication
X Premium|000000|Communication
Patreon|FF424D|Creator
ChatGPT Plus|10A37F|AI
Codex|111111|AI
Claude Pro|D97706|AI
Midjourney|000000|AI
GitHub Copilot|000000|AI
Perplexity|1FB8CD|AI
Gemini|4285F4|AI
GitHub Pro|181717|Developer
GitLab|FC6D26|Developer
Vercel|000000|Developer
Netlify|00C7B7|Developer
DigitalOcean|0080FF|Developer
AWS|FF9900|Developer
Google Cloud|4285F4|Developer
Azure|0078D4|Developer
Swiggy One|FC8019|Food
Zomato Gold|E23744|Food
DoorDash|FF3008|Food
Uber Eats|06C167|Food
Strava|FC4C02|Fitness
Headspace|F47D31|Fitness
Calm|7FCFCF|Fitness
Peloton|000000|Fitness
Cult.fit|FF3366|Fitness
Gym Membership|EF4444|Fitness
Kindle Unlimited|FF9900|Reading
Audible|F8991C|Reading
Medium|000000|Reading
Coursera|0056D2|Learning
Udemy|A435F0|Learning
Skillshare|00FF84|Learning
LinkedIn Premium|0A66C2|Learning
Duolingo|58CC02|Learning
Electricity Bill|F59E0B|Utility
Internet Bill|8B5CF6|Utility
Mobile Recharge|10B981|Utility
Health Insurance|EF4444|Insurance
Car Insurance|3B82F6|Insurance
Life Insurance|10B981|Insurance
Rent|6366F1|Housing
EMI|F59E0B|Finance
SIP|10B981|Investment
Salary|1DB954|Income
Freelance Retainer|3693F3|Income
Rental Income|F59E0B|Income
Dividends & Staking|8B5CF6|Income
Interest Payout|10B981|Income
Consulting|00A8E1|Income
YouTube Creator Payout|FF0000|Income
    """.trimIndent().lines().filter { it.isNotBlank() }.map { line ->
        val (name, color, category) = line.split('|')
        Service(slug(name), name, 0xFF000000 or color.toLong(16), category)
    }

    private val byId = all.associateBy { it.id }

    fun byIcon(icon: String?): Service? = icon?.takeIf { it.startsWith(PREFIX) }?.let { byId[it.removePrefix(PREFIX)] }

    /** Services whose name or category contains [query], ignoring case. */
    fun search(query: String): List<Service> {
        val q = query.trim().lowercase(Locale.ROOT)
        return if (q.isEmpty()) all else all.filter { q in it.name.lowercase(Locale.ROOT) || q in it.category.lowercase(Locale.ROOT) }
    }

    private fun slug(name: String) = name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
}
