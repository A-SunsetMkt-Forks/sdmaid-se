package eu.darken.sdmse.appcleaner.core.forensics.filter

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import eu.darken.sdmse.appcleaner.core.AppCleanerSettings
import eu.darken.sdmse.appcleaner.core.forensics.BaseExpendablesFilter
import eu.darken.sdmse.appcleaner.core.forensics.ExpendablesFilter
import eu.darken.sdmse.appcleaner.core.forensics.sieves.DynamicAppSieve
import eu.darken.sdmse.common.areas.DataArea
import eu.darken.sdmse.common.datastore.value
import eu.darken.sdmse.common.debug.logging.log
import eu.darken.sdmse.common.debug.logging.logTag
import eu.darken.sdmse.common.files.APath
import eu.darken.sdmse.common.files.APathLookup
import eu.darken.sdmse.common.files.GatewaySwitch
import eu.darken.sdmse.common.files.Segments
import eu.darken.sdmse.common.pkgs.Pkg
import eu.darken.sdmse.common.pkgs.toPkgId
import javax.inject.Inject
import javax.inject.Provider

@Reusable
class TelegramFilter @Inject constructor(
    private val dynamicSieveFactory: DynamicAppSieve.Factory,
    private val gatewaySwitch: GatewaySwitch,
) : BaseExpendablesFilter() {

    private lateinit var sieve: DynamicAppSieve

    override suspend fun initialize() {
        log(TAG) { "initialize()" }
        val configs = mutableSetOf<DynamicAppSieve.MatchConfig>()

        DynamicAppSieve.MatchConfig(
            pkgNames = setOf("org.telegram.messenger".toPkgId()),
            areaTypes = setOf(DataArea.Type.SDCARD, DataArea.Type.PUBLIC_DATA),
            ancestors = setOf(
                "Telegram/Telegram Audio",
                "Telegram/Telegram Documents",
                "Telegram/Telegram Images",
                "Telegram/Telegram Video",
                "Telegram/Telegram Stories",
                "org.telegram.messenger/files/Telegram/Telegram Audio",
                "org.telegram.messenger/files/Telegram/Telegram Documents",
                "org.telegram.messenger/files/Telegram/Telegram Images",
                "org.telegram.messenger/files/Telegram/Telegram Video",
                "org.telegram.messenger/files/Telegram/Telegram Stories",
            ),
            exclusions = setOf(".nomedia"),
        ).let { configs.add(it) }

        DynamicAppSieve.MatchConfig(
            pkgNames = setOf("org.telegram.plus".toPkgId()),
            areaTypes = setOf(DataArea.Type.SDCARD),
            ancestors = setOf(
                "Telegram/Telegram Audio",
                "Telegram/Telegram Documents",
                "Telegram/Telegram Images",
                "Telegram/Telegram Video",
                "Telegram/Telegram Stories",
            ),
            exclusions = setOf(".nomedia"),
        ).let { configs.add(it) }

        DynamicAppSieve.MatchConfig(
            pkgNames = setOf("org.thunderdog.challegram".toPkgId()),
            areaTypes = setOf(DataArea.Type.SDCARD, DataArea.Type.PUBLIC_DATA),
            ancestors = setOf(
                "Telegram/Telegram Audio",
                "Telegram/Telegram Documents",
                "Telegram/Telegram Images",
                "Telegram/Telegram Video",
                "Telegram/Telegram Stories",
                "org.thunderdog.challegram/files/documents",
                "org.thunderdog.challegram/files/music",
                "org.thunderdog.challegram/files/videos",
                "org.thunderdog.challegram/files/video_notes",
                "org.thunderdog.challegram/files/animations",
                "org.thunderdog.challegram/files/voice",
                "org.thunderdog.challegram/files/photos",
                "org.thunderdog.challegram/files/stories",
            ),
            exclusions = setOf(".nomedia"),
        ).let { configs.add(it) }

        DynamicAppSieve.MatchConfig(
            pkgNames = setOf("ir.ilmili.telegraph".toPkgId()),
            areaTypes = setOf(DataArea.Type.SDCARD, DataArea.Type.PUBLIC_DATA),
            ancestors = setOf(
                "Telegram/Telegram Audio",
                "Telegram/Telegram Documents",
                "Telegram/Telegram Images",
                "Telegram/Telegram Video",
                "Telegram/Telegram Stories",
                "ir.ilmili.telegraph/files/Telegram/Telegram Audio",
                "ir.ilmili.telegraph/files/Telegram/Telegram Documents",
                "ir.ilmili.telegraph/files/Telegram/Telegram Images",
                "ir.ilmili.telegraph/files/Telegram/Telegram Video",
                "ir.ilmili.telegraph/files/Telegram/Telegram Stories",
            ),
            exclusions = setOf(".nomedia"),
        ).let { configs.add(it) }

        DynamicAppSieve.MatchConfig(
            pkgNames = setOf("org.telegram.messenger.web".toPkgId()),
            areaTypes = setOf(DataArea.Type.SDCARD, DataArea.Type.PUBLIC_DATA),
            ancestors = setOf(
                "Telegram/Telegram Audio",
                "Telegram/Telegram Documents",
                "Telegram/Telegram Images",
                "Telegram/Telegram Video",
                "Telegram/Telegram Stories",
                "org.telegram.messenger.web/files/Telegram/Telegram Audio",
                "org.telegram.messenger.web/files/Telegram/Telegram Documents",
                "org.telegram.messenger.web/files/Telegram/Telegram Images",
                "org.telegram.messenger.web/files/Telegram/Telegram Video",
                "org.telegram.messenger.web/files/Telegram/Telegram Stories",
            ),
            exclusions = setOf(".nomedia"),
        ).let { configs.add(it) }

        sieve = dynamicSieveFactory.create(configs)
    }

    override suspend fun match(
        pkgId: Pkg.Id,
        target: APathLookup<APath>,
        areaType: DataArea.Type,
        segments: Segments
    ): ExpendablesFilter.Match? {
        if (segments.isNotEmpty() && IGNORED_FILES.contains(segments[segments.size - 1])) return null

        return if (segments.isNotEmpty() && sieve.matches(pkgId, areaType, segments)) {
            target.toDeletionMatch()
        } else {
            null
        }
    }

    override suspend fun process(
        targets: Collection<ExpendablesFilter.Match>,
        allMatches: Collection<ExpendablesFilter.Match>
    ): ExpendablesFilter.ProcessResult {
        return deleteAll(
            targets.map { it as ExpendablesFilter.Match.Deletion },
            gatewaySwitch,
            allMatches
        )
    }

    @Reusable
    class Factory @Inject constructor(
        private val settings: AppCleanerSettings,
        private val filterProvider: Provider<TelegramFilter>
    ) : ExpendablesFilter.Factory {
        override suspend fun isEnabled(): Boolean = settings.filterTelegramEnabled.value()
        override suspend fun create(): ExpendablesFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): ExpendablesFilter.Factory
    }

    companion object {
        private val IGNORED_FILES: Collection<String> = listOf(
            ".nomedia",
        )
        private val TAG = logTag("AppCleaner", "Scanner", "Filter", "Telegram")
    }
}