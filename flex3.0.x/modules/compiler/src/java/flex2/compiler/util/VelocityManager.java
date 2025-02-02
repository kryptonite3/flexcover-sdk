////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.util;

import flex.util.SerializedTemplateFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.EventHandler;
import org.apache.velocity.app.event.MethodExceptionEventHandler;
import org.apache.velocity.app.event.ReferenceInsertionEventHandler;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;
import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.UberspectImpl;
import org.apache.velocity.util.introspection.VelPropertyGet;
import org.apache.velocity.util.introspection.VelPropertySet;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p/>
 * Encapsulates Velocity services. Velocity moving parts:
 * <p/>
 * 1. VelocityEngine: template loader, parser pool manager, configuration
 * holder. Intended to be 1:1 with e.g. a servlet context. Note: any template
 * "libraries" containing definitions outside the template itself are associated
 * with instances of VelocityEngine.
 * <p/>
 * 2. VelocityContext: basically a HashMap of names to POJOs, along with some
 * introspection services. When a template is "merged" with a context, $var
 * references in the template become map lookups in the context.
 * <p/>
 * 3. Template: Represented internally as an AST. Standard usage is to parse
 * them from source; so presumably not thread-safe.
 * <p/>
 * We want to support caller-specified libraries, but it's gratuitous to create
 * lots of VelocityEngines, and inconvenient for callers to keep references to
 * them. So we keep a map of VE instances by library config internally. (If the
 * possibility arises of creating too many VEs, a size limiter could be added.)
 * <p/>
 * Standard VelocityManager usage pattern:
 * <p/>
 * <pre>
 *  	Template t = VelocityManager.getTemplate(path[, libs])
 *  	VelocityContext c = VelocityManager.getCodeGenContext([custom util class])
 *  	c.add(name, value) // populate c with name/value pairs used by t
 *  	t.merge(c, w) // w is a Writer
 */
public class VelocityManager
{

	private static final String LOGSYSTEM_CLASS = "flex2.compiler.util.VelocityManager$Logger";

	private static final String STRICT_UBERSPECT_IMPL_CLASS = "flex2.compiler.util.VelocityManager$StrictUberspectImpl";

	private static final String CLASSPATH_RESOURCE_LOADER_CLASS = "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader";

	private static final String UTIL_KEY = "util";

	private static final Map engines = new HashMap();
	private static final Map templates = new HashMap();

	public static long parseTime = 0;
	public static long deserTime = 0;
	public static long initTime = 0;
	public static long mergeTime = 0;

	/**
	 * Create a VelocityEngine instance configured with the given libs string
	 */
	private static final VelocityEngine createEngine(String lib)
	{
		VelocityEngine ve = new VelocityEngine();
		// use our logger, and customize log settings
		ve.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, LOGSYSTEM_CLASS);
		ve.setProperty(Velocity.RUNTIME_LOG_ERROR_STACKTRACE, "false");
		ve.setProperty(Velocity.RUNTIME_LOG_REFERENCE_LOG_INVALID, "true");
		// use our strict introspection
		ve.setProperty(Velocity.UBERSPECT_CLASSNAME, STRICT_UBERSPECT_IMPL_CLASS);
		// load from classpath and file system
		ve.setProperty(Velocity.RESOURCE_LOADER, "file,class");
		// configure class resource loader
		ve.setProperty("class." + Velocity.RESOURCE_LOADER + ".class", CLASSPATH_RESOURCE_LOADER_CLASS);

		// libs are precompiled in later in this function
		ve.setProperty(Velocity.VM_LIBRARY, "");
		// initialize

		try
		{
			ve.init();
		}
		catch (Exception e)
		{
			ThreadLocalToolkit.log(new InitializationError(e.getLocalizedMessage()));
		}

		if (lib != null && ve != null)
		{
			getTemplate(lib, ve);
		}
		return ve;
	}

	/**
	 * Look up the VelocityEngine instance configured with the given libs string
	 */
	private static final VelocityEngine getEngine(String lib)
	{
		String libKey = lib == null ? "" : lib;
		VelocityEngine ve = (VelocityEngine) engines.get(libKey);
		if (ve == null)
			ve = createEngine(lib);
		if (ve != null)
			engines.put(libKey, ve);
		return ve;
	}

	public static Template getTemplate(String path)
	{
		return getTemplate(path, (String) null);
	}

	public static Template getTemplate(String path, String lib)
	{
		VelocityEngine ve = getEngine(lib);
		String templateKey = path + (lib == null ? "" : lib);

		// from what I can tell templates and there parser tree are static (unchanging)
		// data so that this should be thread safe, I think all the transient data comes
		// from the context.
		Template t = (Template) templates.get(templateKey);
		if (t == null)
		{
			t = getTemplate(path, ve);
			templates.put(templateKey, t);
		}
		return t;
	}

	private static Template getTemplate(String path, VelocityEngine ve)
	{
		try
		{
			//long start = System.currentTimeMillis();
			Template t = SerializedTemplateFactory.load(path + "s");
			//long end = System.currentTimeMillis();
			//deserTime += end - start;
			t.setRuntimeServices(ve.getRuntimeServices());
			t.setName(path);
			t.initDocument();
			//initTime += System.currentTimeMillis() - end;
			return t;
		}
		catch (Exception e)
		{
			// any problems here are catastrophic failures
			// FIXME: someone in the mxml fold should review
			throw new RuntimeException(e);
		}
	}


	/**
	 * Create new VelocityContext. Installs Util subclass if passed, otherwise
	 * instance of generic Util. Also, if passed util also implements one or
	 * more velo EventHandlers, we register it as such. Note: currently there's
	 * no reason to create a generic instance for every context, but we may want
	 * to add state - benchmarking stuff etc.
	 */
	public static VelocityContext getCodeGenContext(Util util)
	{
		VelocityContext cx = new VelocityContext();
		if (util == null)
		{
			util = new Util();
		}

		if (util instanceof EventHandler)
		{
			EventCartridge ec = new EventCartridge();
			ec.addEventHandler((EventHandler) util);
			ec.attachToContext(cx);
		}
		cx.put(UTIL_KEY, util);
		return cx;
	}

	/**
	 * return a new VelocityContext with instance of default Util
	 */
	public static VelocityContext getCodeGenContext()
	{
		return getCodeGenContext(null);
	}

	/**
	 * Handles MethodExceptionEvent, and exposes some generic utilities for use
	 * in a context. Users can subclass to add their own purpose-specific stuff
	 */
	public static class Util implements MethodExceptionEventHandler, ReferenceInsertionEventHandler
	{
		static Format dateTimeFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");

		public static long Now()
		{
			return System.currentTimeMillis();
		}

		public static String getTimeStamp()
		{
			return dateTimeFormat.format(new Date(Now()));
		}

		/**
		 * MethodExceptionEventHandler impl
		 */
		public final Object methodException(Class claz, String methodName, Exception e) throws Exception
		{
			ThreadLocalToolkit.log(new InvocationError(claz.getName(), methodName, e.getLocalizedMessage()));
			return null;
		}

		/**
		 * ReferenceInsertionEventHandler impl
		 */
		public Object referenceInsert(String s, Object o)
		{
			if (o == null)
			{
				ThreadLocalToolkit.log(new TemplateReferenceIsNull(s));
			}

			return o;
		}
	}

	/**
	 * Extension of Velocity's standard UberspectImpl. We log an error when an
	 * invocation target mathod is not resolved. Standard implementation's
	 * behavior is to silently return null.
	 */
	public static class StrictUberspectImpl extends UberspectImpl
	{
		public VelPropertyGet getPropertyGet(Object obj, String identifier,
		                                     Info i) throws Exception
		{
			VelPropertyGet getter = super.getPropertyGet(obj, identifier, i);
			// there is no clean way to see if super succeeded
			// @see http://issues.apache.org/bugzilla/show_bug.cgi?id=31742
			try
			{
				getter.getMethodName();
			}
			catch (NullPointerException e)
			{
				ThreadLocalToolkit.log(new GetMethodNotFound(i.getTemplateName(), i.getLine(), i.getColumn(), identifier, obj.getClass().getName()));
			}

			return getter;
		}

		public VelPropertySet getPropertySet(Object obj, String identifier,
		                                     Object arg, Info i) throws Exception
		{
			VelPropertySet setter = super.getPropertySet(obj, identifier, arg, i);
			if (setter == null)
			{
				ThreadLocalToolkit.log(new SetMethodNotFound(i.getTemplateName(), i.getLine(), i.getColumn(), identifier, obj.getClass().getName()));
			}

			return setter;
		}
	}

	/**
	 * route velocity error messages to our logger; also send everything to an
	 * instance of Velo's default log system
	 */
	public static class Logger implements LogSystem
	{
		LogSystem als = null;

		public void init(RuntimeServices rs) throws Exception
		{
			try
			{
				// TODO enable velocity.log based on config setting. Only makes
				// sense once there's an actual meta-compiler instance
				// which holds the trans-compile settings specific to it, and
				// which can hold onto a VelocityManager instance and
				// pass it individual Compilers. Can/should be done as part of
				// the "get default compiler config" facade.
				// uncomment this for velocity.log
				// (als = new AvalonLogSystem()).init(rs);
			}
			catch (NoClassDefFoundError e)
			{
				// ignore
			}
		}

		public void logVelocityMessage(int level, String message)
		{
			if ((level == ERROR_ID) &&
					(!(message.equals("VM #writeWatcher: error : too few arguments to macro. Wanted 1 got 0") ||
						message.equals("VM #writeEvaluationWatcherPart: error : too few arguments to macro. Wanted 2 got 0") ||
						message.equals("VM #writeWatcherBottom: error : too few arguments to macro. Wanted 1 got 0"))))
			{
				ThreadLocalToolkit.logWarning(message);
			}
			else
			{
				//	ThreadLocalToolkit.logDebug(message);
			}

			if (als != null)
			{
				als.logVelocityMessage(level, message);
			}
		}
	}

	// error messages

	public static class InitializationError extends CompilerMessage.CompilerError
	{
		public InitializationError(String message)
		{
			super();
			this.message = message;
		}

		public final String message;
	}

	public static class InvocationError extends CompilerMessage.CompilerError
	{
		public InvocationError(String className, String methodName, String message)
		{
			super();
			this.className = className;
			this.methodName = methodName;
			this.message = message;
		}

		public final String className, methodName, message;
	}

	public static class TemplateReferenceIsNull extends CompilerMessage.CompilerError
	{
		public TemplateReferenceIsNull(String s)
		{
			super();
			this.s = s;
		}

		public final String s;
	}

	public static class GetMethodNotFound extends CompilerMessage.CompilerError
	{
		public GetMethodNotFound(String template, int line, int column, String identifier, String className)
		{
			super();
			this.template = template;
			this.line = line;
			this.column = column;
			this.identifier = identifier;
			this.className = className;
		}

		public final String template, identifier, className;
		public final int line, column;
	}

	public static class SetMethodNotFound extends CompilerMessage.CompilerError
	{
		public SetMethodNotFound(String template, int line, int column, String identifier, String className)
		{
			super();
			this.template = template;
			this.line = line;
			this.column = column;
			this.identifier = identifier;
			this.className = className;
		}

		public final String template, identifier, className;
		public final int line, column;
	}
}
