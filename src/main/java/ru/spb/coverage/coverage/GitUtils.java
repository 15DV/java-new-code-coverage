package ru.spb.coverage.coverage;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import ru.spb.coverage.domain.GitDiff;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class GitUtils {

    private static final String FILE_EXTENSION = ".java";

    private GitUtils() {
    }

    public static List<GitDiff> getChangesInNewBranch(String targetBranch, String gitPath) {
        List<GitDiff> gitDiffs;
        var builder = new FileRepositoryBuilder();

        try (Repository repository = builder.setGitDir(new File(gitPath))
                .readEnvironment()
                .findGitDir()
                .build()) {
            gitDiffs = GitUtils.compareTwoBranches(repository, repository.getFullBranch(), targetBranch);
        } catch (Exception ex) {
            log.error("Could not open git repository or get diffs between branches");
            throw new CoverageTaskException(ex);
        }

        return gitDiffs;
    }

    private static List<GitDiff> compareTwoBranches(Repository repository,
                                                    String currentBranch,
                                                    String targetBranch) throws IOException, GitAPIException {
        log.info("Prepare tree parsers for git branches");
        var currentTreeParser = prepareTreeParser(repository, currentBranch);
        var targetTreeParser = prepareTreeParser(repository, targetBranch);

        var git = new Git(repository);

        var diffFormatter = new DiffFormatter(new ByteArrayOutputStream());
        diffFormatter.setRepository(repository);

        log.info("Calling git diff for target and current branches");
        var diffEntries = git.diff()
                .setOldTree(targetTreeParser)
                .setNewTree(currentTreeParser)
                .call();

        var changes = new ArrayList<GitDiff>();

        for (var diffEntry : diffEntries) {
            var editList = diffFormatter.toFileHeader(diffEntry).toEditList();
            var className = diffEntry.getNewPath();

            if (!StringUtils.endsWith(className, FILE_EXTENSION)) {
                continue;
            }

            if (className.contains("/")) {
                className = StringUtils.substringAfterLast(diffEntry.getNewPath(), "/");
            }

            var changesBuilder = GitDiff.builder().changedFileName(className);

            for (var edit : editList) {
                //We are not interested in deleted lines
                if (edit.getType() != Edit.Type.DELETE) {
                    var change = changesBuilder.beginChanges(edit.getBeginB())
                            .endChanges(edit.getEndB())
                            .build();
                    changes.add(change);
                }
            }
        }

        return changes;
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String ref) throws IOException {
        var head = repository.exactRef(ref);
        var walk = new RevWalk(repository);
        if (head == null) {
            throw new CoverageTaskException("The HEAD of ref is NULL");
        }
        var commit = walk.parseCommit(head.getObjectId());
        var tree = walk.parseTree(commit.getTree().getId());

        var treeParser = new CanonicalTreeParser();
        var reader = repository.newObjectReader();

        treeParser.reset(reader, tree.getId());

        walk.dispose();

        return treeParser;
    }
}
