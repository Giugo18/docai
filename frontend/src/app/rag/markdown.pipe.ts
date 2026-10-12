import { Pipe, PipeTransform } from '@angular/core';
import { marked } from 'marked';

/** Markdown → HTML. L'HTML va usato con [innerHTML]: Angular lo sanitizza. */
@Pipe({ name: 'markdown' })
export class MarkdownPipe implements PipeTransform {
  transform(testo: string | null | undefined): string {
    if (!testo) return '';
    return marked.parse(testo, { async: false, gfm: true, breaks: true }) as string;
  }
}