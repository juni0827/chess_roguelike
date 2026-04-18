package com.chessroguelike.event

import com.chessroguelike.engine.Move

/**
 * Central publish / subscribe message bus.
 *
 * The [EventBus] decouples the core game logic from presentation
 * (UI, rendering, sound) by acting as a one-way communication
 * channel.  Logic systems **publish** events; view-layer
 * subscribers **react** without the publisher knowing about them.
 *
 * Usage:
 * ```
 * val bus = EventBus()
 * bus.subscribe(OnPieceMoved::class) { event ->
 *     renderMoveAnimation(event.move)
 * }
 * bus.publish(OnPieceMoved(move, isPlayer = true))
 * ```
 *
 * Thread-safety: the current implementation is **not**
 * thread-safe.  All publish / subscribe calls should happen on
 * the same thread (typically the game / UI thread).
 */
class EventBus {

    /** All subscriber callbacks, keyed by the concrete event type. */
    private val subscribers =
        mutableMapOf<Class<out GameDomainEvent>, MutableList<(GameDomainEvent) -> Unit>>()

    /**
     * Register a handler for a specific event type.
     * Returns a [Subscription] handle that can be used to [unsubscribe].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : GameDomainEvent> subscribe(
        eventType: Class<T>,
        handler: (T) -> Unit
    ): Subscription {
        val wrapper: (GameDomainEvent) -> Unit = { handler(it as T) }
        subscribers.getOrPut(eventType) { mutableListOf() }.add(wrapper)
        return Subscription(eventType, wrapper)
    }

    /**
     * Kotlin-friendly subscribe using reified type parameter.
     */
    inline fun <reified T : GameDomainEvent> subscribe(
        noinline handler: (T) -> Unit
    ): Subscription = subscribe(T::class.java, handler)

    /**
     * Remove a previously registered subscription.
     */
    fun unsubscribe(subscription: Subscription) {
        subscribers[subscription.eventType]?.remove(subscription.handler)
    }

    /**
     * Remove **all** subscriptions for every event type.
     */
    fun clear() {
        subscribers.clear()
    }

    /**
     * Publish an event to all subscribers of its concrete type.
     */
    fun publish(event: GameDomainEvent) {
        subscribers[event.javaClass]?.forEach { handler -> handler(event) }
    }

    /**
     * Publish a batch of events in order.
     */
    fun publishAll(events: Iterable<GameDomainEvent>) {
        events.forEach { publish(it) }
    }

    /** Opaque handle returned by [subscribe]. */
    data class Subscription(
        val eventType: Class<out GameDomainEvent>,
        val handler: (GameDomainEvent) -> Unit
    )
}
