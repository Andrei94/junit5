/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static java.util.Collections.emptySet;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.MAINTAINED;

import java.util.Optional;
import java.util.Set;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.TestDescriptor;

/**
 * A <em>node</em> within the execution hierarchy.
 *
 * @param <C> the type of {@code EngineExecutionContext} used by the
 * {@code HierarchicalTestEngine}
 * @since 1.0
 * @see HierarchicalTestEngine
 */
@API(status = MAINTAINED, since = "1.0", consumers = "org.junit.platform.engine.support.hierarchical")
public interface Node<C extends EngineExecutionContext> {

	/**
	 * Prepare the supplied {@code context} prior to execution.
	 *
	 * <p>The default implementation returns the supplied {@code context} unmodified.
	 *
	 * @see #cleanUp(EngineExecutionContext)
	 */
	default C prepare(C context) throws Exception {
		return context;
	}

	/**
	 * Cleanup the supplied {@code context} after execution.
	 *
	 * <p>The default implementation does nothing.
	 *
	 * @since 1.1
	 * @see #prepare(EngineExecutionContext)
	 */
	default void cleanUp(C context) throws Exception {
	}

	/**
	 * Determine if the execution of the supplied {@code context} should be
	 * <em>skipped</em>.
	 *
	 * <p>The default implementation returns {@link SkipResult#doNotSkip()}.
	 */
	default SkipResult shouldBeSkipped(C context) throws Exception {
		return SkipResult.doNotSkip();
	}

	/**
	 * Execute the <em>before</em> behavior of this node.
	 *
	 * <p>This method will be called once <em>before</em> {@linkplain #execute
	 * execution} of this node.
	 *
	 * @param context the context to execute in
	 * @return the new context to be used for children of this node
	 *
	 * @see #execute
	 * @see #after
	 */
	default C before(C context) throws Exception {
		return context;
	}

	/**
	 * Execute the <em>behavior</em> of this node.
	 *
	 * <p>Containers typically do not implement this method since the
	 * {@link HierarchicalTestEngine} handles execution of their children.
	 *
	 * <p>The supplied {@code dynamicTestExecutor} may be used to submit
	 * additional dynamic tests for immediate execution.
	 *
	 * @param context the context to execute in
	 * @param dynamicTestExecutor the executor to submit dynamic tests to
	 * @return the new context to be used for children of this node and for the
	 * <em>after</em> behavior of the parent of this node, if any
	 *
	 * @see #before
	 * @see #after
	 */
	default C execute(C context, DynamicTestExecutor dynamicTestExecutor) throws Exception {
		return context;
	}

	/**
	 * Execute the <em>after</em> behavior of this node.
	 *
	 * <p>This method will be called once <em>after</em> {@linkplain #execute
	 * execution} of this node.
	 *
	 * @param context the context to execute in
	 *
	 * @see #before
	 * @see #execute
	 */
	default void after(C context) throws Exception {
	}

	/**
	 * Get the set of {@linkplain ExclusiveResource resources} required to
	 * execute this node.
	 *
	 * @return the set of resources required by this node; never {@code null}
	 * but potentially empty
	 * @see ExclusiveResource
	 * @since 1.3
	 */
	@API(status = EXPERIMENTAL, since = "1.3", consumers = "org.junit.platform.engine.support.hierarchical")
	default Set<ExclusiveResource> getExclusiveResources() {
		return emptySet();
	}

	/**
	 * Get the preferred of {@linkplain ExecutionMode execution mode} for
	 * parallel execution of this node.
	 *
	 * @return the preferred execution mode of this node; never {@code null}
	 * @see ExecutionMode
	 * @since 1.3
	 */
	@API(status = EXPERIMENTAL, since = "1.3", consumers = "org.junit.platform.engine.support.hierarchical")
	default ExecutionMode getExecutionMode() {
		return ExecutionMode.CONCURRENT;
	}

	/**
	 * The result of determining whether the execution of a given {@code context}
	 * should be <em>skipped</em>.
	 *
	 * @see Node#shouldBeSkipped(EngineExecutionContext)
	 */
	class SkipResult {

		private static final SkipResult alwaysExecuteSkipResult = new SkipResult(false, null);

		private final boolean skipped;
		private final Optional<String> reason;

		/**
		 * Factory for creating <em>skipped</em> results.
		 *
		 * <p>A context that is skipped will be not be executed.
		 *
		 * @param reason the reason why the context should be skipped
		 * @return a skipped {@code SkipResult} with the given reason
		 */
		public static SkipResult skip(String reason) {
			return new SkipResult(true, reason);
		}

		/**
		 * Factory for creating <em>do not skip</em> results.
		 *
		 * <p>A context that is not skipped will be executed as normal.
		 *
		 * @return a <em>do not skip</em> {@code SkipResult}
		 */
		public static SkipResult doNotSkip() {
			return alwaysExecuteSkipResult;
		}

		private SkipResult(boolean skipped, String reason) {
			this.skipped = skipped;
			this.reason = Optional.ofNullable(reason);
		}

		/**
		 * Whether execution of the context should be skipped.
		 *
		 * @return {@code true} if the execution should be skipped
		 */
		public boolean isSkipped() {
			return this.skipped;
		}

		/**
		 * Get the reason why execution of the context should be skipped,
		 * if available.
		 */
		public Optional<String> getReason() {
			return this.reason;
		}

		@Override
		public String toString() {
			// @formatter:off
			return new ToStringBuilder(this)
					.append("skipped", this.skipped)
					.append("reason", this.reason.orElse("<unknown>"))
					.toString();
			// @formatter:on
		}
	}

	/**
	 * Executor for additional, dynamic test descriptors discovered during
	 * execution of a {@link Node}.
	 *
	 * <p>The test descriptors will be executed by the same
	 * {@link HierarchicalTestExecutor} that executes the submitting node.
	 *
	 * <p>This interface is not intended to be implemented by clients.
	 *
	 * @see Node#execute(EngineExecutionContext, DynamicTestExecutor)
	 * @see HierarchicalTestExecutor
	 */
	interface DynamicTestExecutor {

		/**
		 * Submit a dynamic test descriptor for immediate execution.
		 *
		 * @param testDescriptor the test descriptor to be executed
		 */
		void execute(TestDescriptor testDescriptor);

	}

	/**
	 * Supported execution modes for parallel execution.
	 *
	 * @see Node#getExecutionMode()
	 * @since 1.3
	 */
	@API(status = EXPERIMENTAL, since = "1.3", consumers = "org.junit.platform.engine.support.hierarchical")
	enum ExecutionMode {

		/**
		 * Force execution in same thread as the parent node.
		 */
		SAME_THREAD,

		/**
		 * Allow concurrent execution with any other node.
		 */
		CONCURRENT
	}

}
