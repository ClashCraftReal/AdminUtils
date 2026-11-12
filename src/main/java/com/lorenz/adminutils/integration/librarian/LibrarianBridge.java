package com.lorenz.adminutils.integration.librarian;

import java.util.Optional;

/**
 * Boundary for a future Librarian hotbar bridge.
 *
 * <p>Deliberately unimplemented: Librarian publishes no stable API, so this interface
 * exists to keep the call sites decoupled until a real contract is available. Every
 * method must stay safe to call when Librarian is absent.
 */
public interface LibrarianBridge {
	boolean available();

	Optional<String> describeTarget();
}
