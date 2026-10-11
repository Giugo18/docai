import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChiediDocumenti } from './chiedi-documenti';

describe('ChiediDocumenti', () => {
  let component: ChiediDocumenti;
  let fixture: ComponentFixture<ChiediDocumenti>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChiediDocumenti],
    }).compileComponents();

    fixture = TestBed.createComponent(ChiediDocumenti);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
