package com.jrm.onboarding.splash

/**
 * Manages the chain of ad loading operations
 * Makes it easy to extend and modify the loading sequence
 */
class SplashAdLoadingChain(
    private val adLoader: SplashAdLoader
) {

    /**
     * Execute L1 -> L2 -> Ob1 chain
     */
    fun executeL1Chain() {
        adLoader.preloadL1Native {
            executeL2Chain()
        }
    }

    /**
     * Execute L2 -> Ob1 chain
     */
    fun executeL2Chain() {
        adLoader.preloadL2Native {
            adLoader.preloadOb1()
        }
    }

    /**
     * Execute Ob1 only
     */
    fun executeOb1Chain() {
        adLoader.preloadOb1()
    }
}

