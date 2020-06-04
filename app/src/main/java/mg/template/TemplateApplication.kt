package mg.template

import mg.template.core.base.BaseApplication
import mg.template.core.build.BuildParams
import mg.template.data.di.dataDiModule
import mg.template.di.appDiModule
import mg.template.login.loginDiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TemplateApplication : BaseApplication() {

    override val buildParams: BuildParams = BuildParamsImpl()

    override fun initDI() {
        startKoin {
            androidLogger()
            androidContext(this@TemplateApplication)
            modules(
                listOf(
                    appDiModule,
                    dataDiModule,
                    loginDiModule
                )
            )
        }
    }
}