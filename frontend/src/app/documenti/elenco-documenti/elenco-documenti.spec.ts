import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ElencoDocumenti } from './elenco-documenti';

describe('ElencoDocumenti', () => {
  let component: ElencoDocumenti;
  let fixture: ComponentFixture<ElencoDocumenti>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ElencoDocumenti],
    }).compileComponents();

    fixture = TestBed.createComponent(ElencoDocumenti);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
