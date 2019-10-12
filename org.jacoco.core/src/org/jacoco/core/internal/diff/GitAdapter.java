/*******************************************************************************
 * Copyright (c) 2009, 2019 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.internal.diff;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.*;
import java.util.*;

/**
 * Git操作类
 */
public class GitAdapter {
    private Git git;
    private Repository repository;
    private String gitFilePath;

    //  Git授权
    private static UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider;

    public GitAdapter(String gitFilePath) {
        this.gitFilePath = gitFilePath;
        this.initGit(gitFilePath);
    }
    private void initGit(String gitFilePath) {
        try {
            git = Git.open(new File(gitFilePath));
            repository = git.getRepository();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getGitFilePath() {
        return gitFilePath;
    }

    public Git getGit() {
        return git;
    }

    public Repository getRepository() {
        return repository;
    }

    /**
     * git授权。需要设置拥有所有权限的用户
     * @param username  git用户名
     * @param password  git用户密码
     */
    public static void setCredentialsProvider(String username, String password) {
        if(usernamePasswordCredentialsProvider == null || !usernamePasswordCredentialsProvider.isInteractive()){
            usernamePasswordCredentialsProvider = new UsernamePasswordCredentialsProvider(username,password);
        }
    }

    /**
     * 获取指定分支的指定文件内容
     * @param branchName        分支名称
     * @param javaPath          文件路径
     * @return  java类
     * @throws IOException
     */
    public String getBranchSpecificFileContent(String branchName, String javaPath) throws IOException {
        Ref branch = repository.exactRef("refs/heads/" + branchName);
        ObjectId objId = branch.getObjectId();
        RevWalk walk = new RevWalk(repository);
        RevTree tree = walk.parseTree(objId);
        return  getFileContent(javaPath,tree,walk);
    }

    /**
     * 获取指定分支指定Tag版本的指定文件内容
     * @param tagRevision       Tag版本
     * @param javaPath          件路径
     * @return  java类
     * @throws IOException
     */
    public String getTagRevisionSpecificFileContent(String tagRevision, String javaPath) throws IOException {
        ObjectId objId = repository.resolve(tagRevision);
        RevWalk walk = new RevWalk(repository);
        RevCommit revCommit = walk.parseCommit(objId);
        RevTree tree = revCommit.getTree();
        return  getFileContent(javaPath,tree,walk);
    }
    
    /**
     * 获取指定分支指定的指定文件内容
     * @param javaPath      件路径
     * @param tree          git RevTree
     * @param walk          git RevWalk
     * @return  java类
     * @throws IOException
     */
    private String getFileContent(String javaPath,RevTree tree,RevWalk walk) throws IOException {
        TreeWalk treeWalk = TreeWalk.forPath(repository, javaPath, tree);
        ObjectId blobId = treeWalk.getObjectId(0);
        ObjectLoader loader = repository.open(blobId);
        byte[] bytes = loader.getBytes();
        walk.dispose();
        return new String(bytes);
    }

    /**
     * 分析分支树结构信息
     * @param localRef      本地分支
     * @return
     * @throws IOException
     */
    public AbstractTreeIterator prepareTreeParser(Ref localRef) throws IOException {
        RevWalk walk = new RevWalk(repository);
        RevCommit commit = walk.parseCommit(localRef.getObjectId());
        RevTree tree = walk.parseTree(commit.getTree().getId());
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        ObjectReader reader = repository.newObjectReader();
        treeParser.reset(reader, tree.getId());
        walk.dispose();
        return treeParser;
    }
    /**
     * 切换分支
     * @param branchName    分支名称
     * @throws GitAPIException GitAPIException
     */
    public void checkOut(String branchName) throws GitAPIException {
        //  切换分支
        git.checkout().setCreateBranch(false).setName(branchName).call();
    }

    /**
     * 更新分支代码
     * @param localRef      本地分支
     * @param branchName    分支名称
     * @throws GitAPIException GitAPIException
     */
    public void checkOutAndPull(Ref localRef, String branchName) throws GitAPIException {
        boolean isCreateBranch = localRef == null;
        if (!isCreateBranch && checkBranchNewVersion(localRef)) {
            return;
        }
        //  切换分支
        git.checkout().setCreateBranch(isCreateBranch).setName(branchName).setStartPoint("origin/" + branchName).setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM).call();
        //  拉取最新代码
        git.pull().setCredentialsProvider(usernamePasswordCredentialsProvider).call();
    }

    /**
     * 判断本地分支是否是最新版本。目前不考虑分支在远程仓库不存在，本地存在
     * @param localRef  本地分支
     * @return  boolean
     * @throws GitAPIException GitAPIException
     */
    private boolean checkBranchNewVersion(Ref localRef) throws GitAPIException {
        String localRefName = localRef.getName();
        String localRefObjectId = localRef.getObjectId().getName();
        //  获取远程所有分支
        Collection<Ref> remoteRefs = git.lsRemote().setCredentialsProvider(usernamePasswordCredentialsProvider).setHeads(true).call();
        for (Ref remoteRef : remoteRefs) {
            String remoteRefName = remoteRef.getName();
            String remoteRefObjectId = remoteRef.getObjectId().getName();
            if (remoteRefName.equals(localRefName)) {
                if (remoteRefObjectId.equals(localRefObjectId)) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }
}
