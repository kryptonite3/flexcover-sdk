////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.config;

import flash.localization.LocalizationManager;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

/**
 * @author Roger Gonzalez
 *
 */
public class CommandLineConfigurator
{
	public static final String SOURCE_COMMAND_LINE = "command line";
	
	
    /**
     * parse - buffer up configuration vals from the command line
     *
     * @param buffer        the configuration buffer to hold the results
     * @param defaultvar    the variable name where the trailing loose args go
     * @param args          the command line
     * @throws ConfigurationException
     */
    public static void parse( final ConfigurationBuffer buffer,
                              final String defaultvar,
                              final String[] args)
            throws ConfigurationException
    {
        assert defaultvar == null || buffer.isValidVar( defaultvar ) : "coding error: config must provide default var " + defaultvar;

        Map aliases = getAliases( buffer );
        final int START = 1;
        final int ARGS = 2;
        final int EXEC = 3;
        final int DONE = 4;


        int i = 0, iStart = 0, iEnd = 0;
        String var = null;
        int varArgCount = -2;
        List argList = new LinkedList();
        Set vars = new HashSet();
        boolean append = false;
        boolean dash = true;

        int mode = START;

        while (mode != DONE)
        {
            switch (mode)
            {
                case START:
                {
                	iStart = i;
                	
                    if (args.length == i)
                    {
                        mode = DONE;
                        break;
                    }
                    // expect -var, --, or the beginning of default args

                    mode = ARGS;
                    varArgCount = -2;

                    if (args[i].equals("--"))
                    {
                        dash = false;
                        if (defaultvar != null)
                            var = defaultvar;
                        else
                            mode = START;
                        ++i;
                    }
                    else if (dash && args[i].startsWith("+"))
                    {
                        String token = null;
                        int c = (args[i].length() > 1 && args[i].charAt( 1 ) == '+')? 2 : 1;    // gnu-style?

                        int equals = args[i].indexOf( '=' );
                        String rest = null;
                        if (equals != -1)
                        {
                            rest = args[i].substring( equals + 1 );
                            token = args[i++].substring( c, equals );
                        }
                        else
                        {
                            token = args[i++].substring( c );
                        }
                        if (equals != -1)
                        {
                        	iEnd = i;
                            buffer.setToken( token, rest );
                            buffer.addPosition(token, iStart, iEnd);
                        }
                        else
                        {
                            if (i == args.length)
                            {
                                throw new ConfigurationException.Token( ConfigurationException.Token.INSUFFICIENT_ARGS,
                                                                        token, var, source, -1 );
                            }
                            rest = args[i++];
                            iEnd = i;
                            buffer.setToken( token, rest );
                            buffer.addPosition(token, iStart, iEnd);
                        }
                        mode = START;
                        break;
                    }
                    else if (dash && isAnArgument(args[i]))
                    {
                        int c = (args[i].length() > 1 && args[i].charAt( 1 ) == '-')? 2 : 1;    // gnu-style?

                        int plusequals = args[i].indexOf( "+=" );
                        int equals = args[i].indexOf( '=' );
                        String rest = null;
                        if (plusequals != -1)
                        {
                            rest = args[i].substring( plusequals + 2 );
                            var = args[i++].substring( c, plusequals );
                            append = true;
                        }
                        else if (equals != -1)
                        {
                            rest = args[i].substring( equals + 1 );
                            var = args[i++].substring( c, equals );
                        }
                        else
                        {
                            var = args[i++].substring( c );
                        }

                        if (aliases.containsKey( var ))
                            var = (String) aliases.get( var );

                        if (!buffer.isValidVar( var ))
                        {
                            throw new ConfigurationException.UnknownVariable( var, source, -1 );
                        }

                        if (equals != -1)
                        {
                            if ((rest == null) || (rest.length() == 0))
                            {
                            	iEnd = i;
                                buffer.clearVar( var, source, -1 );
                                buffer.addPosition(var, iStart, iEnd);
                                mode = START;
                            }
                            else
                            {
                                String seps = null;
                                if (buffer.getInfo(var).isPath())
                                {
                                    seps = "[," + File.pathSeparatorChar + "]";
                                }
                                else {
                                	seps = ",";
                                }
                                
                                String[] tokens = rest.split(seps);
                                argList.addAll(Arrays.asList(tokens));
                                varArgCount = buffer.getVarArgCount( var );
                                mode = EXEC;
                            }
                        }

                    }
                    else
                    {
                        if (defaultvar != null)
                        {
                            // don't increment i, let ARGS pick it up.
                            var = defaultvar;
                        }
                        else
                        {
                            throw new ConfigurationException.UnexpectedDefaults( null, null, -1 );
                        }
                    }
                    break;
                }
                case ARGS:
                {
                    if (varArgCount == -2)
                    {
                        if (isBoolean( buffer, var ))
                        {
                            varArgCount = 0;
                            mode = EXEC;
                            break;
                        }
                        else
                        {
                            varArgCount = buffer.getVarArgCount( var );
                        }
                    }
                    assert varArgCount >= -1;   // just in case the getVarArgCount author was insane.

                    if (args.length == i)
                    {
                        mode = EXEC;
                        break;
                    }

                    boolean greedy = buffer.getInfo( var ).isGreedy();

                    // accumulating non-command arguments...

                    
                    // check for a terminator on our accumulated parameter list
                    if (!greedy && dash && isAnArgument(args[i]))
                    {
                        if (varArgCount == -1)
                        {
                            // we were accumulating an unlimited set of args, a new var terminates that.
                            mode = EXEC;
                            break;
                        }
                        throw new ConfigurationException.IncorrectArgumentCount( varArgCount, argList.size(), var, source, -1 );
                    }

                    // this test is a little hairy:
                    //    "The key is that the parameter before the "default" parameter takes an
                    //     unlimited number of parameters: mxmlc -rsl 1.swf 2.swf test.mxml" -dloverin
                    if ((varArgCount == -1)
                            && !greedy
                            && (defaultvar != null)
                            && !defaultvar.equals(var)
                            && !vars.contains( defaultvar )
                            && ((args.length - i) > 1)
                            && buffer.getInfo( defaultvar ) != null)
                    {
                        // look for a terminating argument, if none,
                        // then the end of the list cannot be determined (it's ambiguous)
                        boolean ok = false;
                        for (int j = i + 1; j < args.length; ++j)
                        {
                            if (dash && isAnArgument(args[j]))
                            {
                                ok = true;
                                break;
                            }
                        }
                        if (!ok)
                        {
                            throw new ConfigurationException.AmbiguousParse( defaultvar, var, source, -1 );
                        }
                    }

                    argList.add( args[i++] );
                    if (argList.size() == varArgCount)
                    {
                        mode = EXEC;
                    }

                    break;
                }
                case EXEC:
                {
                    if ((varArgCount != -1) && (argList.size() != varArgCount))
                    {
                        throw new ConfigurationException.IncorrectArgumentCount( varArgCount, argList.size(), var, source, -1 );
                    }
                    if (varArgCount == 0)       // boolean flag fakery...
                        argList.add( "true" );

                    if (vars.contains( var ))
                    {
                        if ((defaultvar != null) && var.equals( defaultvar ))
                        {
                            // we could perhaps accumulate the defaults spread out through
                            // the rest of the flags, but for now we'll call this illegal.
                            throw new ConfigurationException.InterspersedDefaults( var, source, -1 );
                        }
                    }
                    iEnd = i;
                    buffer.setVar( var, new LinkedList( argList ), source, -1, null, append );
                    buffer.addPosition(var, iStart, iEnd);
                    append = false;
                    vars.add( var );
                    argList.clear();
                    mode = START;
                    break;
                }
                case DONE:
                {
                    assert false;
                    break;
                }
            }
        }
    }
    
    /**
     * Given a string like "-foo" or "-5" or "-123.mxml", this determines whether
     * the string is an argument or... not an argument (e.g. numeral)
     */
    private static boolean isAnArgument(final String arg)
    {
        return (arg.startsWith("-") &&
                // if the first character after a dash is numeric, this is not
                // an argument, it is a parameter (and therefore non-terminating)
               (arg.length() > 1) && !Character.isDigit(arg.charAt(1)));
    }
    
    private static Map getAliases( ConfigurationBuffer buffer )
    {
        Map aliases = new HashMap();
        aliases.putAll( buffer.getAliases() );
        for (Iterator it = buffer.getVarIterator(); it.hasNext(); )
        {
            String varname = (String) it.next();

            if (varname.indexOf( '.' ) == -1)
                continue;

            String leafname = varname.substring( varname.lastIndexOf( '.' ) + 1 );
            if (aliases.containsKey( leafname ))
                continue;
            aliases.put( leafname, varname );
        }

        return aliases;
    }
    private static boolean isBoolean( ConfigurationBuffer buffer, String var )
    {
        ConfigurationInfo info = buffer.getInfo( var );

        if (info.getArgCount() > 1)
            return false;

        Class c = info.getArgType( 0 );

        return ((c == boolean.class) || (c == Boolean.class));
    }

    public static String brief( String program, String defaultvar, LocalizationManager l10n, String l10nPrefix )
    {
        Map params = new HashMap();
        params.put( "defaultVar", defaultvar );
        params.put( "program", program );
        return l10n.getLocalizedTextString( l10nPrefix + ".Brief", params );
    }


    static public String usage( String program, String defaultVar, ConfigurationBuffer cfgbuf, Set keywords, LocalizationManager lmgr, String l10nPrefix )
    {
        // FIXME (probably a FOL, unfortunately) - this is totally biased to western languages.

        Map aliases = getAliases( cfgbuf );

        Map sesaila = new HashMap();
        for (Iterator it = aliases.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry e = (Map.Entry) it.next();
            sesaila.put( e.getValue(), e.getKey() );
        }

        TreeSet printSet = new TreeSet();

        boolean all = false;
        boolean advanced = false;
        boolean details = false;
        boolean syntax = false;
        boolean printaliases = false;

        // figure out behavior..
        Set newSet = new HashSet();
        for (Iterator kit = keywords.iterator(); kit.hasNext();)
        {
            String keyword = (String) kit.next();

            if (keyword.equals( "list" ))
            {
                all = true;
                newSet.add( "*" );
            }
            else if (keyword.equals( "advanced" ))
            {
                advanced = true;
                if (keywords.size() == 1)
                {
                    all = true;
                    newSet.add( "*" );
                }
            }
            else if (keyword.equals( "details" ))
            {
                details = true;
            }
            else if (keyword.equals( "syntax" ))
            {
                syntax = true;
            }
            else if (keyword.equals( "aliases" ))
            {
                printaliases = true;
            }
            else
            {
                details = true;
                newSet.add( keyword );
            }
        }
        if (syntax)
        {
            List lines = ConfigurationBuffer.formatText( getSyntaxDescription( program, defaultVar, advanced, lmgr,  l10nPrefix ), 78 );
            StringBuffer text = new StringBuffer( 512 );
            for (Iterator it = lines.iterator(); it.hasNext();)
            {
                text.append( it.next() );
                text.append( "\n" );
            }
            return text.toString();
        }
        keywords = newSet;

        // accumulate set to print
        for (Iterator kit = keywords.iterator(); kit.hasNext();)
        {
            String keyword = ((String) kit.next()).toLowerCase();

            for (Iterator varit = cfgbuf.getVarIterator(); varit.hasNext(); )
            {
                String var = (String) varit.next();
                ConfigurationInfo info = cfgbuf.getInfo( var );

                String description = getDescription( cfgbuf, var, lmgr, l10nPrefix);

                if ((all
                        || (var.indexOf( keyword ) != -1)
                        || ((description != null) && (description.toLowerCase().indexOf( keyword ) != -1))
                        || (keyword.matches( var ) )
                        || ((sesaila.get( var ) != null) && ((String)sesaila.get( var )).indexOf( keyword ) != -1))
                     && (!info.isHidden())
                     && (advanced || !info.isAdvanced()))
                {
                    if (printaliases && sesaila.containsKey( var ))
                        printSet.add( sesaila.get( var ));
                    else
                        printSet.add( var );
                }
                else
                {
                    /*
                    for (int i = 0; i < info.getAliases().length; ++i)
                    {
                        String alias = info.getAliases()[i];
                        if (alias.indexOf( keyword ) != -1)
                        {
                            printSet.add( var );
                        }
                    }
                    */
                }
            }
        }

        StringBuffer output = new StringBuffer( 1024 );

        if (printSet.size() == 0)
        {
            String nkm = lmgr.getLocalizedTextString( l10nPrefix + ".NoKeywordsMatched" );
            output.append( nkm );
            output.append( "\n" );
        }
        else for (Iterator it = printSet.iterator(); it.hasNext();)
        {
            String avar = (String) it.next();
            String var = avar;
            if (aliases.containsKey( avar ))
                var = (String) aliases.get( avar );

            ConfigurationInfo info = cfgbuf.getInfo( var );
            assert info != null;

            output.append( "-" );
            output.append( avar );

            int count = cfgbuf.getVarArgCount( var );
            if ((count >= 1) && (!isBoolean( cfgbuf, var )))
            {
                for (int i = 0; i < count; ++i)
                {
                    output.append( " <" );
                    output.append( cfgbuf.getVarArgName( var, i ) );
                    output.append( ">" );
                }
            }
            else if (count == -1)
            {
                String last = "";
                for (int i = 0; i < 5; ++i)
                {
                    String argname = cfgbuf.getVarArgName( var, i );
                    if (!argname.equals( last ))
                    {
                        output.append( " [" );
                        output.append( argname );
                        output.append( "]" );
                        last = argname;
                    }
                    else
                    {
                        output.append( " [...]" );
                        break;
                    }
                }
            }

            output.append( "\n" );

            if (details)
            {
                StringBuffer description = new StringBuffer( 160 );
                if (printaliases)
                {
                    if (aliases.containsKey( avar ))
                    {
                        String fullname = lmgr.getLocalizedTextString( l10nPrefix + ".FullName" );
                        description.append( fullname );
                        description.append( " -" );
                        description.append( aliases.get( avar ));
                        description.append( "\n" );
                    }
                }
                else if (sesaila.containsKey( var ))
                {
                    String alias = lmgr.getLocalizedTextString( l10nPrefix + ".Alias" );
                    description.append( alias );
                    description.append( " -" );
                    description.append( sesaila.get( var ));
                    description.append( "\n" );
                }

                String d = getDescription(cfgbuf, var, lmgr, l10nPrefix);
                if (var.equals( "help" ) && (printSet.size() > 2))
                {
                    String helpKeywords = lmgr.getLocalizedTextString( l10nPrefix + ".HelpKeywords" );
                    description.append( helpKeywords );
                }
                else if (d != null)
                    description.append( d );

                String flags = "";
                if (info.isAdvanced())
                {
                    String advancedString = lmgr.getLocalizedTextString( l10nPrefix + ".Advanced" );
                    flags += (((flags.length() == 0)? " (" : ", ") + advancedString );
                }
                if (info.allowMultiple())
                {
                    String repeatableString = lmgr.getLocalizedTextString( l10nPrefix + ".Repeatable" );
                    flags += (((flags.length() == 0)? " (" : ", ") + repeatableString );
                }
                if ((defaultVar != null) && var.equals( defaultVar ))
                {
                    String defaultString = lmgr.getLocalizedTextString( l10nPrefix + ".Default" );
                    flags += (((flags.length() == 0)? " (" : ", ") + defaultString );
                }
                if (flags.length() != 0)
                {
                    flags += ")";
                }
                description.append( flags );


                List descriptionLines = ConfigurationBuffer.formatText( description.toString(), 70 );

                for (Iterator descit = descriptionLines.iterator(); descit.hasNext();)
                {
                    output.append( "    " );
                    output.append( (String) descit.next() );
                    output.append( "\n" );
                }
            }
        }
        return output.toString();
    }

    public static String getDescription( ConfigurationBuffer buffer, String var, LocalizationManager l10n, String l10nPrefix )
    {
        String key = (l10nPrefix == null)? var : (l10nPrefix + "." + var);
        String description = l10n.getLocalizedTextString( key, null );

        return description;
    }


    public static String getSyntaxDescription( String program, String defaultVar, boolean advanced, LocalizationManager l10n, String l10nPrefix )
    {
        Map params = new HashMap();
        params.put("defaultVar", defaultVar);
        params.put("program", program);

        String key = l10nPrefix + "." + (advanced? "AdvancedSyntax" : "Syntax");
        String text = l10n.getLocalizedTextString( key, params );

        if (text == null)
        {
            text = "No syntax help available, try '-help list' to list available configuration variables.";
            assert false : "Localized text for syntax description not found!";
        }
        return text;
    }

    public static final String source = SOURCE_COMMAND_LINE;
}
