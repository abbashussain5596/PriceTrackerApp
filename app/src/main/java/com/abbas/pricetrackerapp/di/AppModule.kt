package com.abbas.pricetrackerapp.di

import com.abbas.pricetrackerapp.data.datasource.WebSocketDataSource
import com.abbas.pricetrackerapp.data.datasource.WebSocketDataSourceImpl
import com.abbas.pricetrackerapp.data.repository.PriceRepositoryImpl
import com.abbas.pricetrackerapp.domain.repository.PriceRepository
import com.abbas.pricetrackerapp.domain.usecase.GetStockSymbolsUseCase
import com.abbas.pricetrackerapp.domain.usecase.ObserveConnectionStateUseCase
import com.abbas.pricetrackerapp.domain.usecase.ObserveFeedRunningStateUseCase
import com.abbas.pricetrackerapp.domain.usecase.ObservePriceUpdatesUseCase
import com.abbas.pricetrackerapp.domain.usecase.StartPriceFeedUseCase
import com.abbas.pricetrackerapp.domain.usecase.StopPriceFeedUseCase
import com.abbas.pricetrackerapp.presentation.viewmodel.PriceFeedViewModel
import com.abbas.pricetrackerapp.presentation.viewmodel.SymbolDetailsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Data layer – singletons so the WebSocket connection and price state are shared
    single<WebSocketDataSource> { WebSocketDataSourceImpl() }
    single<PriceRepository> { PriceRepositoryImpl(get()) }

    // Domain – use cases are stateless, so factory (new instance per injection site)
    factory { GetStockSymbolsUseCase(get()) }
    factory { ObservePriceUpdatesUseCase(get()) }
    factory { ObserveConnectionStateUseCase(get()) }
    factory { ObserveFeedRunningStateUseCase(get()) }
    factory { StartPriceFeedUseCase(get()) }
    factory { StopPriceFeedUseCase(get()) }

    // Presentation – ViewModels; SavedStateHandle is injected automatically by Koin
    // when koinViewModel() is called inside a Navigation composable
    viewModel {
        PriceFeedViewModel(
            getStockSymbolsUseCase = get(),
            observePriceUpdatesUseCase = get(),
            observeConnectionStateUseCase = get(),
            observeFeedRunningStateUseCase = get(),
            startPriceFeedUseCase = get(),
            stopPriceFeedUseCase = get()
        )
    }

    viewModel {
        SymbolDetailsViewModel(
            savedStateHandle = get(),
            observePriceUpdatesUseCase = get(),
            observeConnectionStateUseCase = get(),
            startPriceFeedUseCase = get()
        )
    }
}
