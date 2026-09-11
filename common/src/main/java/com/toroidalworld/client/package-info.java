/**
 * Everything that may load on the client side alone — {@code client.engine} the client's half of the engine,
 * {@code client.shape} and {@code client.options} the screens — because this package boundary is what keeps a
 * {@code Screen} off a dedicated server. The public half of the same boundary is {@code api.v1.client}, which a shape
 * registers its screens through and which is entered from the client side alone for the same reason.
 */
package com.toroidalworld.client;
