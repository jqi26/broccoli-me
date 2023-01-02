package com.example.broccolime

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.net.CronetProviderInstaller
import dagger.Component
import dagger.Module
import dagger.Provides
import org.chromium.net.CronetEngine
import javax.inject.Singleton
import kotlin.system.exitProcess


@Module
class AppModule(var context: Context) {

    @Provides
    @Singleton
    fun providesCronetEngine(): CronetEngine {
        val myBuilder = CronetEngine.Builder(context)
        return myBuilder.build()
    }
}

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(activity: MainActivity) // void inject(MyFragment fragment);
    // void inject(MyService service);
}

class BroccoliMeApplication : Application() {
    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        CronetProviderInstaller.installProvider(this).addOnCompleteListener {
            if (it.isSuccessful) {
                appComponent =
                    DaggerAppComponent.builder() // list of modules that are part of this component need to be created here too
                        .appModule(AppModule(this))
                        .build()
            } else {
                Log.w("Cronet", "Unable to load Cronet from Play Services", it.exception)
                exitProcess(1)
            }
        }

        // Dagger%COMPONENT_NAME%


        // If a Dagger 2 component does not have any constructor arguments for any of its modules,
        // then we can use .create() as a shortcut instead:
        //  mAppComponent = com.codepath.dagger.components.DaggerAppComponent.create();
    }
}