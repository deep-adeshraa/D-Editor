package com.crio.qcharm.ds;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.crio.qcharm.request.EditRequest;
import com.crio.qcharm.request.PageRequest;
import com.crio.qcharm.request.SearchReplaceRequest;
import com.crio.qcharm.request.SearchRequest;
import com.crio.qcharm.request.UndoRequest;

public class SourceFileHandlerArrayListImpl implements SourceFileHandler {

  SourceFileVersionArrayListImpl sourceFileVersionArrayListImpl;
  List<String> lines;
  Stack<SourceFileVersionArrayListImpl> undoStack = new Stack<>();
  Stack<SourceFileVersionArrayListImpl> redoStack = new Stack<>();
  String filename;

  public SourceFileHandlerArrayListImpl(String fileName) {
  }

  @Override
  public Page loadFile(FileInfo fileInfo) {
    this.sourceFileVersionArrayListImpl = new SourceFileVersionArrayListImpl(fileInfo);
    this.filename = fileInfo.getFileName();
    this.lines = sourceFileVersionArrayListImpl.getAllLines();
    Page page = new Page(lines.size() <= 50 ? lines : lines.subList(0, 50), 0, fileInfo.getFileName(),
        new Cursor(0, 0));
    return page;
  }

  @Override
  public Page getPrevLines(PageRequest pageRequest) {
    return this.sourceFileVersionArrayListImpl.getLinesBefore(pageRequest);
  }

  @Override
  public Page getNextLines(PageRequest pageRequest) {
    return this.sourceFileVersionArrayListImpl.getLinesAfter(pageRequest);
  }

  @Override
  public Page getLinesFrom(PageRequest pageRequest) {
    return this.sourceFileVersionArrayListImpl.getLinesFrom(pageRequest);
  }

  @Override
  public SourceFileVersion getLatestSourceFileVersion(String fileName) {
    return this.sourceFileVersionArrayListImpl;
  }

  @Override
  public List<Cursor> search(SearchRequest searchRequest) {
    List<Cursor> answer = new ArrayList<>();
    try {
      answer = this.sourceFileVersionArrayListImpl.getCursors(searchRequest);
      return answer;
    } catch (Exception e) {
      return null;
    }
  }

  CopyBuffer copyBuffer;
  SourceFileVersion copyVersion;

  @Override
  public void setCopyBuffer(CopyBuffer copyBuffer) {
    this.copyBuffer = copyBuffer;
  }

  @Override
  public CopyBuffer getCopyBuffer() {
    return this.copyBuffer;
  }

  @Override
  public SourceFileVersion cloneObj(SourceFileVersion ver) {
    this.copyVersion = ver;
    return copyVersion;
  }

  @Override
  public void editLines(EditRequest editRequest) {
    this.undoStack.push(new SourceFileVersionArrayListImpl(this.sourceFileVersionArrayListImpl));
    int start = editRequest.getStartingLineNo();
    int end = editRequest.getEndingLineNo();
    UpdateLines update = new UpdateLines(start, end - start - 1, editRequest.getNewContent(),
        editRequest.getCursorAt());
    this.sourceFileVersionArrayListImpl.apply(update);
  }

  @Override
  public void searchReplace(SearchReplaceRequest searchReplaceRequest) {
    this.undoStack.push(new SourceFileVersionArrayListImpl(this.sourceFileVersionArrayListImpl));
    SearchReplace searchRequest = new SearchReplace(searchReplaceRequest.getStartingLineNo(),
        searchReplaceRequest.getLength(), new Cursor(), searchReplaceRequest.getPattern(),
        searchReplaceRequest.getNewPattern());
    this.sourceFileVersionArrayListImpl.apply(searchRequest);
  }

  @Override
  public void undo(UndoRequest undoRequest) {
    if (!this.undoStack.isEmpty()) {
      this.redoStack.push(this.sourceFileVersionArrayListImpl);
      this.sourceFileVersionArrayListImpl = this.undoStack.pop();
    }
  }

  @Override
  public void redo(UndoRequest undoRequest) {
    if (!this.redoStack.isEmpty()) {
      this.undoStack.push(this.sourceFileVersionArrayListImpl);
      this.sourceFileVersionArrayListImpl = this.redoStack.pop();
    }
  }

  // TODO: CRIO_TASK_MODULE_UNDO_REDO
  // Input:
  // None
  // Description:
  // Return the page that was in view as of this edit.
  // 1. starting line number -should be same as it was in the last change
  // 2. Cursor - should return to the same position as it was in the last change
  // 3. Number of lines - should be same as it was in the last change.

  public Page getCursorPage() {
    return null;
  }

}
