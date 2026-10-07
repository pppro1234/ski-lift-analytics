package skiliftanalytics.sequential;

import skiliftanalytics.concurrent.SkiDataProcessor;

/**
 * Sequential CLI entry point. Optional arguments select an input CSV path
 * and output directory; by default reports are written under {@code out/}.
 */
public class Main {
    public static void main(String[] args) {
        String[] delegatedArgs = new String[args.length + 1];
        delegatedArgs[0] = "sequential";
        System.arraycopy(args, 0, delegatedArgs, 1, args.length);
        SkiDataProcessor.main(delegatedArgs);
    }
}
