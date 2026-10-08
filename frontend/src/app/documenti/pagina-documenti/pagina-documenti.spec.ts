import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaginaDocumenti } from './pagina-documenti';

describe('PaginaDocumenti', () => {
  let component: PaginaDocumenti;
  let fixture: ComponentFixture<PaginaDocumenti>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaginaDocumenti],
    }).compileComponents();

    fixture = TestBed.createComponent(PaginaDocumenti);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
