import { TestBed } from '@angular/core/testing';
import { DocumentiApi } from './documenti-api';

describe('DocumentiApi', () => {
  let service: DocumentiApi;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DocumentiApi);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
