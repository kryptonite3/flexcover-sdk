This is a source-code-only project containing patches to the Flex SDK for code coverage instrumentation, to be used in conjunction with the Flexcover project.

To avoid checking in the entire SDK, various branches etc. only contain these patched subdirectories of the `modules/` subdirectory that pertain specifically to the mxmlc compiler:

  * `asc/`: the AS3 compiler core
  * `compiler/`: the mxmlc/compc compiler tool overlay
  * `swfutils/`: utility code common to both

The repository itself is structured as follows:

  * `vendor/`: pristine imported copies of the above modules from the Flex SDK, for diffing and merging purposes.
  * `trunk/`: modified versions of the vendor copies.  These might more rightly have been termed 'branches' since there are separate areas on the trunk for each Flex SDK codeline.

The most recent version of the Flexcover SDK is on `trunk/flex3.2.0` and reflects Build 3958 of the Flex 3.2 SDK.  The diffs in this patch can be recovered by this command:

```
svn diff https://flexcover-sdk.googlecode.com/svn/vendor/3.2.0_r3958 https://flexcover-sdk.googlecode.com/svn/trunk/flex3.2.0 
```

## Creating a new revision ##

There are two approaches: merge diffs between Flex SDK versions to the flexcover-sdk trunk, or merge flexcover-sdk diffs to a pristine Flex SDK.  They both work, but it's generally easier to merge smaller diffs, so this decision depends on how much the relevant bits of the SDK have evolved since the last time we patched it.

Say the next version of the SDK is not all that different relative to the last one patched.  It would make sense to check in the new SDK on vendor/XXX (where XXX is the prior version used as a basis for patching), and then merge this diff to the corresponding trunk/YYY codelines (where YYY is the codeline containing the patch of XXX).

But if the SDK is more radically changed, then instead one might check in the entire pristine SDK to vendor/ZZZ, svn cp this to trunk/ZZZ and merge the diffs from vendor/XXX to trunk/YYY.