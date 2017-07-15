package com.wenhui.coroutines

/* All the functional interfaces */

internal typealias Function0<R> = () -> R
internal typealias Function1<T, R> = (T) -> R
internal typealias Function2<T1, T2, R> = (T1, T2) -> R
internal typealias Function3<T1, T2, T3, R> = (T1, T2, T3) -> R
internal typealias Function4<T1, T2, T3, T4, R> = (T1, T2, T3, T4) -> R